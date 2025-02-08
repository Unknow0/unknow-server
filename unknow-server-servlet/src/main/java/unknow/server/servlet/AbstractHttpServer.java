package unknow.server.servlet;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletException;
import unknow.server.servlet.http11.HandlerHttp;
import unknow.server.servlet.impl.FilterConfigImpl;
import unknow.server.servlet.impl.ServletConfigImpl;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.impl.session.NoSessionFactory;
import unknow.server.servlet.utils.EventManager;
import unknow.server.servlet.utils.ServletManager;
import unknow.server.util.data.ArrayMap;

/**
 * Abstract server
 * 
 * @author unknow
 */
public abstract class AbstractHttpServer /*extends NIOServerBuilder*/ {
//	private Opt http;
//	private Opt https;
//	private Opt keystore;
//	private Opt keystorePass;
//	private Opt vhost;
//	private Opt execMin;
//	private Opt execMax;
//	private Opt execIdle;
//	private Opt keepAlive;

	/** the servlet context */
	protected ServletContextImpl ctx;
	/** the servlet manager */
	protected ServletManager manager;
	/** the events */
	protected EventManager events;

	/** @return the servlet manager */
	protected abstract ServletManager createServletManager();

	/** @return the event manager */
	protected abstract EventManager createEventManager();

	/**
	 * @param vhost the vhost
	 * @return the context
	 */
	protected abstract ServletContextImpl createContext(String vhost);

	/** @return the servlets */
	protected abstract ServletConfigImpl[] createServlets();

	/** @return the filters */
	protected abstract FilterConfigImpl[] createFilters();

//	@Override
//	protected void beforeParse() {
//		http = withOpt("http-addr").withCli(Option.builder("a").longOpt("http-addr").hasArg().desc("address to bind http to").build());
//		https = withOpt("https-addr").withCli(Option.builder().longOpt("https-addr").hasArg().desc("address to bind https to").build());
//
//		keystore = withOpt("keystore").withCli(Option.builder().longOpt("keystore").hasArg().desc("keystore to use for https").build());
//		keystorePass = withOpt("keystore-pass").withCli(Option.builder().longOpt("keystore-pass").hasArg().desc("passphrase for the keystore").build());
//
//		vhost = withOpt("vhost").withCli(Option.builder().longOpt("vhost").hasArg().desc("public vhost seen by the servlet, default to the binded address").build());
//		execMin = withOpt("exec-min").withCli(Option.builder().longOpt("exec-min").hasArg().desc("min number of exec thread to use").build()).withValue("0");
//		execMax = withOpt("exec-max").withCli(Option.builder().longOpt("exec-max").hasArg().desc("max number of exec thread to use").build())
//				.withValue(Integer.toString(Integer.MAX_VALUE));
//		execIdle = withOpt("exec-idle").withCli(Option.builder().longOpt("exec-idle").hasArg().desc("max idle time for exec thread in seconds").build()).withValue("60");
//		keepAlive = withOpt("keepalive")
//				.withCli(Option.builder().longOpt("keepalive").hasArg().desc("max time to keep idle keepalive connection in seconds, -1: no keep alive, 0: infinite").build())
//				.withValue("2");
//	}

//	@Override
//	protected void process(NIOServer server, CommandLine cli) throws Exception {
//		String ks = keystore.value(cli);
//		if (ks == null)
//			http = http.withValue(":8080");
//		else
//			https = https.withValue(":8443");
//
//		InetSocketAddress addressHttp = parseAddr(cli, http, "");
//		InetSocketAddress addressHttps = parseAddr(cli, https, "");
//
//		String value = cli.getOptionValue(vhost.name());
//		if (value == null)
//			value = addressHttp == null ? addressHttps.getHostName() : addressHttp.getHostString();
//
//		manager = createServletManager();
//		events = createEventManager();
//		ctx = createContext(value);
//
//		loadInitializer();
//		manager.initialize(ctx, createServlets(), createFilters());
//		events.fireContextInitialized(ctx);
//
//		SSLContext sslContext = addressHttps == null ? null : sslContext(ks, keystorePass.value(cli));
//
//		AtomicInteger i = new AtomicInteger();
//		ExecutorService executor = new ThreadPoolExecutor(parseInt(cli, execMin, 0), parseInt(cli, execMax, 0), parseInt(cli, execIdle, 0), TimeUnit.SECONDS,
//				new SynchronousQueue<>(), r -> {
//					Thread t = new Thread(r, "exec-" + i.getAndIncrement());
//					t.setDaemon(true);
//					return t;
//				});
//		int keepAliveIdle = parseInt(cli, keepAlive, -1) * 1000;
//		if (addressHttps != null)
//			server.bind(addressHttps, key -> new NIOConnectionSSL(key, new HttpConnection(executor, ctx, manager, events, keepAliveIdle), sslContext));
//		if (addressHttp != null)
//			server.bind(addressHttp, key -> new NIOConnectionPlain(key, new HttpConnection(executor, ctx, manager, events, keepAliveIdle)));
//	}

	private final SslContext sslContext(String keystore, String password)
			throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
//		KeyStore ks = KeyStore.getInstance("JKS");
//		try (InputStream is = Files.newInputStream(Paths.get(keystore))) {
//			ks.load(is, null);
//		}
//
//		KeyManagerFactory keyManager = KeyManagerFactory.getInstance("SunX509");
//		keyManager.init(ks, password == null ? null : password.toCharArray());
//
//		TrustManagerFactory trust = TrustManagerFactory.getInstance("SunX509");
//		trust.init(ks);
		ApplicationProtocolConfig alpn = new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE, SelectedListenerFailureBehavior.ACCEPT,
				Arrays.asList("h2", "http/1.1"));
//		return SslContextBuilder.forServer(keyManager).trustManager(trust).applicationProtocolConfig(alpn).build();
		SelfSignedCertificate cert = new SelfSignedCertificate();
		return SslContextBuilder.forServer(cert.key(), cert.cert()).applicationProtocolConfig(alpn).build();

//		SSLContext sslContext = SSLContext.getInstance("TLS");
//		sslContext.init(keyManager.getKeyManagers(), trust.getTrustManagers(), null);
//		return sslContext;
	}

	/**
	 * find and call initializer
	 * 
	 * @throws ServletException on error
	 */
	protected void loadInitializer() throws ServletException {
		for (ServletContainerInitializer i : ServiceLoader.load(ServletContainerInitializer.class)) {
			i.onStartup(null, ctx);
		}
	}

	/**
	 * do build and run the server
	 * 
	 * @param arg the main arguments
	 * @throws Exception on error
	 */
	public void process(String[] arg) throws Exception {

		manager = createServletManager();
		events = createEventManager();
		ctx = new ServletContextImpl("test", "vhost", new ArrayMap<>(new String[] { "ctx" }, new String[] { "value" }), manager, events, new NoSessionFactory(),
				new ArrayMap<>(new String[] {}, new String[] {}),
				new ArrayMap<>(
						new String[] { "7z", "aac", "avi", "bmp", "bz", "bz2", "css", "csv", "gif", "htm", "html", "ico", "ics", "jar", "jpeg", "jpg", "js", "json", "jsp",
								"mid", "midi", "mpeg", "oga", "ogv", "ogx", "otf", "pdf", "png", "rar", "rtf", "sh", "svg", "tar", "tif", "tiff", "ts", "ttf", "wav", "weba",
								"webm", "webp", "woff", "woff2", "xhtml", "xml", "zip" },
						new String[] { "application/x-7z-compressed", "audio/aac", "video/x-msvideo", "image/bmp", "application/x-bzip", "application/x-bzip2", "text/css",
								"text/csc", "image/gif", "text/html", "text/html", "image/x-icon", "text/calendar", "application/java-archive", "image/jpeg", "image/jpeg",
								"application/javascript", "application/json", "text/html", "audio/midi", "audio/midi", "video/mpeg", "audio/ogg", "video/ogg",
								"application/ogg", "font/otf", "application/pdf", "image/png", "application/x-rar-compressed", "application/rtf", "application/x-sh",
								"image/svg+xml", "application/x-tar", "image/tiff", "image/tiff", "application/typescript", "font/ttf", "audio/x-wav", "audio/webm",
								"video/webm", "image/webp", "font/woff", "font/woff2", "application/xhtml+xml", "application/xml", "application/zip" }));

		loadInitializer();
		manager.initialize(ctx, createServlets(), createFilters());
		events.fireContextInitialized(ctx);

		SslContext ssl = sslContext(null, null);

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap b = new ServerBootstrap();
			b.option(ChannelOption.SO_BACKLOG, 1024);
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO)).childHandler(new Initialize(ctx, ssl));

			Channel ch = b.bind(8080).sync().channel();
			b.bind(8443).sync();
			System.out.println("Started " + ch);
			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

//		NIOServer nioServer = build(arg);
//		try {
//			nioServer.start();
//			nioServer.await();
//		} finally {
//			nioServer.stop();
//			nioServer.await();
//			events.fireContextDestroyed();
//		}
	}

	private static final class Initialize extends ChannelInitializer<SocketChannel> {
		private final ServletContextImpl servletContext;
		private final SslContext ssl;

		public Initialize(ServletContextImpl servletContext, SslContext ssl) {
			this.servletContext = servletContext;
			this.ssl = ssl;
		}

		@Override
		protected void initChannel(SocketChannel ch) {
			System.out.println(ch);
			ChannelPipeline p = ch.pipeline();
			p.addLast("idle", new IdleStateHandler(0, 0, 10));
			if (ssl != null && ch.localAddress().getPort() == 8443)
				p.addLast(ssl.newHandler(ch.alloc()), new APNLHandler(servletContext));
			else
				p.addLast(new HttpServerCodec(), new HandlerHttp(servletContext));
		}
	}

	private static final class APNLHandler extends ApplicationProtocolNegotiationHandler {
		private final ServletContextImpl servletContext;

		public APNLHandler(ServletContextImpl servletContext) {
			super(ApplicationProtocolNames.HTTP_1_1);
			this.servletContext = servletContext;
		}

		@Override
		protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
			if (ApplicationProtocolNames.HTTP_2.equals(protocol))
				ctx.pipeline().addLast(Http2FrameCodecBuilder.forServer().autoAckPingFrame(true).autoAckSettingsFrame(true).build())
						.addLast(new Http2MultiplexHandler(new Http2Frame2Http()));
			else
				ctx.pipeline().addLast(new HttpServerCodec(), new HandlerHttp(servletContext));
		}
	}

	private static final class Http2Frame2Http extends SimpleChannelInboundHandler<Http2Frame> implements Runnable {
		private static final ExecutorService POOL = Executors.newCachedThreadPool();
		private static final HttpVersion HTTP_2 = new HttpVersion("HTTP", 2, 0, true);
		private static final ByteBuf C = Unpooled.copiedBuffer("Hello Netty HTTP!".getBytes());

		private ChannelHandlerContext ctx;
		private ServletRequestImpl req;

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
			System.out.println(evt);
			if (evt instanceof IdleStateEvent)
				ctx.close();
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Http2Frame msg) {
			System.out.println(msg);
			if (msg instanceof Http2HeadersFrame)
				process(ctx, (Http2HeadersFrame) msg);
		}

		private void process(ChannelHandlerContext ctx, Http2HeadersFrame msg) {
			Http2Headers headers = msg.headers();
			HttpRequest req = new DefaultHttpRequest(HTTP_2, HttpMethod.valueOf(headers.method().toString()), headers.path().toString());

			HttpHeaders h = req.headers();
			for (Entry<CharSequence, CharSequence> e : headers) {
				if (e.getKey().charAt(0) != ':')
					h.add(e.getKey(), e.getValue());
			}

			POOL.submit(this);
		}

		@Override
		public void run() {
			DefaultHttp2Headers headers = new DefaultHttp2Headers();
			headers.status("200");
			headers.add(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(C.capacity()));
			ctx.write(new DefaultHttp2HeadersFrame(headers));

			ctx.writeAndFlush(new DefaultHttp2DataFrame(C, true));
		}
	}
}
