package unknow.server.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletException;
import unknow.server.servlet.http11.Http11Handler;
import unknow.server.servlet.http2.Http2Handler;
import unknow.server.servlet.impl.FilterConfigImpl;
import unknow.server.servlet.impl.ServletConfigImpl;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.session.NoSessionFactory;
import unknow.server.servlet.utils.EventManager;
import unknow.server.servlet.utils.ServletManager;
import unknow.server.util.data.ArrayMap;

/**
 * Abstract server
 * 
 * @author unknow
 */
public abstract class AbstractHttpServer {

	private static final Option help = Option.builder("h").longOpt("help").desc("show this help").build();
	private static final Option shutdown = Option.builder().longOpt("shutdown").argName("port|addr:port").hasArg().type(Integer.class)
			.desc("addr:port to gracefuly shutdown the server").build();
	private static final Option http = Option.builder("a").longOpt("http-addr").argName("port|addr:port").hasArg().desc("addr:port to bind http to").build();
	private static final Option https = Option.builder().longOpt("https-addr").argName("port|addr:port").hasArg().desc("addr:port to bind https to").build();
	private static final Option vhost = Option.builder().longOpt("vhost").hasArg().desc("public vhost seen by the servlet, default to the binded address").build();
	private static final Option keepAlive = Option.builder().longOpt("keepalive").hasArg()
			.desc("max time to keep idle keepalive connection in seconds, -1: no keep alive, 0: infinite").build();
	private static final Option keystore = Option.builder().longOpt("keystore").hasArg().desc("keystore to use for https").build();
	private static final Option keypass = Option.builder().longOpt("keypass").hasArg().desc("passphrase for the keystore").build();

	/** @return the contextName */
	protected abstract String contextName();

	/** @return the servlet manager */
	protected abstract ServletManager createServletManager();

	/** @return the event manager */
	protected abstract EventManager createEventManager();

	/** @return the servlets */
	protected abstract ServletConfigImpl[] createServlets(ServletContextImpl ctx);

	/** @return the filters */
	protected abstract FilterConfigImpl[] createFilters(ServletContextImpl ctx);

	/** @return the context init params */
	protected abstract ArrayMap<String> initParam();

	/** @return the locales to encoding mapping */
	protected abstract ArrayMap<String> locales();

	/** @return the extention to mime-type mapping */
	protected abstract ArrayMap<String> mimeTypes();

	protected ExecutorService pool() {
		return Executors.newCachedThreadPool(new DefaultThreadFactory("request-executor", true));
	}

	/**
	 * find and call initializer
	 * 
	 * @throws ServletException on error
	 */
	protected void loadInitializer(ServletContextImpl ctx) throws ServletException {
		for (ServletContainerInitializer i : ServiceLoader.load(ServletContainerInitializer.class)) {
			i.onStartup(null, ctx);
		}
	}

	private ServletContextImpl initializeContext(String vhost) throws ServletException {
		ServletManager manager = createServletManager();
		EventManager events = createEventManager();
		ServletContextImpl ctx = new ServletContextImpl(contextName(), vhost, initParam(), manager, events, new NoSessionFactory(), locales(), mimeTypes());

		loadInitializer(ctx);
		manager.initialize(ctx, createServlets(ctx), createFilters(ctx));
		events.fireContextInitialized(ctx);
		return ctx;
	}

	/**
	 * do build and run the server
	 * 
	 * @param arg the main arguments
	 * @throws Exception on error
	 */
	public void process(String[] arg) throws Exception {

		CommandLine cli = parseCli(arg);

		int keepAliveTime = Integer.parseInt(cli.getOptionValue(keepAlive, "2"));

		InetSocketAddress addressShutdown = parseAddr(cli, shutdown, "127.0.0.1");
		InetSocketAddress addressHttp = parseAddr(cli, http, "");
		InetSocketAddress addressHttps = parseAddr(cli, https, "");

		if (addressHttp == null && addressHttps == null)
			addressHttp = new InetSocketAddress(8080);

		String vhostName = cli.getOptionValue(vhost, addressHttp != null ? addressHttp.getHostString() : addressHttps != null ? addressHttps.getHostName() : "");

		ExecutorService pool = pool();
		SslContext ssl = sslContext(cli.getOptionValue(keystore), cli.getOptionValue(keypass));

		ServletContextImpl ctx = initializeContext(vhostName);

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

		ShutdownHandler shutdownHandler = new ShutdownHandler(allChannels);

		try {
			ServerBootstrap b = new ServerBootstrap();
			b.option(ChannelOption.SO_BACKLOG, 1024);
			b.childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.SO_KEEPALIVE, true);
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new Initialize(pool, allChannels, ctx, ssl, keepAliveTime, addressHttp, addressHttps, addressShutdown, shutdownHandler));

			if (addressHttp != null)
				allChannels.add(b.bind(addressHttp).sync().channel());
			if (addressHttps != null)
				allChannels.add(b.bind(addressHttps).sync().channel());
			if (addressShutdown != null)
				allChannels.add(b.bind(addressShutdown).sync().channel());

			allChannels.newCloseFuture().sync(); // wait only on server channel
			allChannels.newCloseFuture().sync(); // wait on all other channel
		} finally {
			bossGroup.shutdownGracefully(0, 30, TimeUnit.SECONDS).syncUninterruptibly();
			workerGroup.shutdownGracefully(0, 30, TimeUnit.SECONDS).syncUninterruptibly();
			ctx.events().fireContextDestroyed();
		}
	}

	private static final CommandLine parseCli(String[] arg) {
		Options opts = new Options().addOption(help).addOption(vhost).addOption(http).addOption(https).addOption(shutdown).addOption(vhost).addOption(keystore)
				.addOption(keepAlive).addOption(keypass);

		CommandLine cli = null;
		try {
			cli = new DefaultParser().parse(opts, arg);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
		}
		if (cli == null || cli.hasOption(help)) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.setWidth(100);
			helpFormatter.printHelp("nioserver", opts);
			System.exit(2);
		}
		return cli;
	}

	private static final SslContext sslContext(String keystore, String password)
			throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, UnrecoverableKeyException {
		ApplicationProtocolConfig alpn = new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE, SelectedListenerFailureBehavior.ACCEPT,
				Arrays.asList("h2", "http/1.1"));

		if (keystore == null) {
			SelfSignedCertificate cert = new SelfSignedCertificate();
			return SslContextBuilder.forServer(cert.key(), cert.cert()).applicationProtocolConfig(alpn).build();
		}

		KeyStore ks = KeyStore.getInstance("JKS");
		try (InputStream is = Files.newInputStream(Paths.get(keystore))) {
			ks.load(is, null);
		}

		KeyManagerFactory keyManager = KeyManagerFactory.getInstance("SunX509");
		keyManager.init(ks, password == null ? null : password.toCharArray());

		TrustManagerFactory trust = TrustManagerFactory.getInstance("SunX509");
		trust.init(ks);
		return SslContextBuilder.forServer(keyManager).trustManager(trust).applicationProtocolConfig(alpn).build();
	}

	/**
	 * parse an option to a inetaddress in the format (port, :port or addr:port)
	 * 
	 * @param cli the command line
	 * @param o the option
	 * @param defaultHost the default host to use
	 * @return the value
	 * @throws IllegalArgumentException if the value is malformed
	 */
	public static InetSocketAddress parseAddr(CommandLine cli, Option o, String defaultHost) {
		String host = defaultHost;
		String a = cli.getOptionValue(o);
		if (a == null)
			return null;
		int i = a.indexOf(':');
		if (i == 0)
			a = a.substring(1);
		else if (i > 0) {
			host = a.substring(0, i);
			a = a.substring(i + 1);
		}
		try {
			int port = Integer.parseInt(a);
			if (host == null || host.isEmpty())
				return new InetSocketAddress(port);
			return new InetSocketAddress(host, port);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			throw new IllegalArgumentException(o + " invalid value " + cli.getOptionValue(o));
		}
	}

	private static final class Initialize extends ChannelInitializer<SocketChannel> {
		private final ExecutorService pool;
		private final ChannelGroup allChannels;
		private final ServletContextImpl servletContext;
		private final SslContext ssl;
		private final int keepAlive;

		private final InetSocketAddress http;
		private final InetSocketAddress https;
		private final InetSocketAddress shutdown;
		private final ShutdownHandler shutdownHandler;

		public Initialize(ExecutorService pool, ChannelGroup allChannels, ServletContextImpl servletContext, SslContext ssl, int keepAlive, InetSocketAddress http,
				InetSocketAddress https, InetSocketAddress shutdown, ShutdownHandler shutdownHandler) {
			this.pool = pool;
			this.allChannels = allChannels;
			this.servletContext = servletContext;
			this.ssl = ssl;
			this.keepAlive = keepAlive;

			this.http = http;
			this.https = https;
			this.shutdown = shutdown;
			this.shutdownHandler = shutdownHandler;
		}

		@Override
		protected void initChannel(SocketChannel ch) {
			allChannels.add(ch);
			ChannelPipeline p = ch.pipeline();
			InetSocketAddress addr = ch.localAddress();
			if (match(http, addr))
				p.addLast(new HttpServerCodec()).addLast("http11", new Http11Handler(pool, servletContext, keepAlive));
			else if (match(https, addr))
				p.addLast(ssl.newHandler(ch.alloc()), new APNLHandler(pool, servletContext, keepAlive));
			else if (match(shutdown, addr)) {
				allChannels.remove(ch);
				p.addLast(shutdownHandler);
			} else
				ch.close();

		}

		private static boolean match(InetSocketAddress a, InetSocketAddress b) {
			if (a == null || a.getPort() != b.getPort())
				return false;
			return a.getAddress().isAnyLocalAddress() || a.getAddress().equals(b.getAddress());
		}
	}

	private static final class APNLHandler extends ApplicationProtocolNegotiationHandler {
		private static final Http2Settings H2SETTINGS = new Http2Settings().initialWindowSize(Integer.MAX_VALUE);

		private final ExecutorService pool;
		private final ServletContextImpl servletContext;
		private final int keepAlive;

		public APNLHandler(ExecutorService pool, ServletContextImpl servletContext, int keepAlive) {
			super(ApplicationProtocolNames.HTTP_1_1);
			this.pool = pool;
			this.servletContext = servletContext;
			this.keepAlive = keepAlive;
		}

		@Override
		protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
			if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
				Http2Handler http2Handler = new Http2Handler(pool, servletContext, keepAlive);
				ctx.pipeline().addLast(Http2FrameCodecBuilder.forServer().initialSettings(H2SETTINGS).build(), http2Handler.outbound(), http2Handler);
			} else {
				Http11Handler http11Handler = new Http11Handler(pool, servletContext, keepAlive);
				ctx.pipeline().addLast(new HttpServerCodec(), http11Handler.outbound(), http11Handler);
			}
			ctx.fireChannelActive();
		}
	}

	public static final class ShutdownHandler extends SimpleChannelInboundHandler<ByteBuf> {
		private static final Logger logger = LoggerFactory.getLogger(ShutdownHandler.class);

		private final ChannelGroup allChannels;

		public ShutdownHandler(ChannelGroup allChannels) {
			this.allChannels = allChannels;
		}

		@Override
		public boolean isSharable() {
			return true;
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
			String receivedMessage = msg.toString(StandardCharsets.UTF_8).trim();
			logger.info("{} {}", ctx.channel(), receivedMessage);
			if ("shutdown".equals(receivedMessage))
				allChannels.close().addListener(c -> ctx.close());
			else
				ctx.close();
		}
	}

}
