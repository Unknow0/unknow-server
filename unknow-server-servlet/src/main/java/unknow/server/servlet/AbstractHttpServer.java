package unknow.server.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ServiceLoader;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletException;
import unknow.server.nio.NIOConnectionPlain;
import unknow.server.nio.NIOConnectionSSL;
import unknow.server.nio.NIOServer;
import unknow.server.nio.NIOServerBuilder;
import unknow.server.servlet.impl.FilterConfigImpl;
import unknow.server.servlet.impl.ServletConfigImpl;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.utils.EventManager;
import unknow.server.servlet.utils.ServletManager;

/**
 * Abstract server
 * 
 * @author unknow
 */
public abstract class AbstractHttpServer extends NIOServerBuilder {
	private Opt http;
	private Opt https;
	private Opt keystore;
	private Opt keystorePass;
	private Opt vhost;
	private Opt keepAlive;

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

	@Override
	protected void beforeParse() {
		http = withOpt("http-addr").withCli(Option.builder("a").longOpt("http-addr").hasArg().desc("address to bind http to").build());
		https = withOpt("https-addr").withCli(Option.builder().longOpt("https-addr").hasArg().desc("address to bind https to").build());

		keystore = withOpt("keystore").withCli(Option.builder().longOpt("keystore").hasArg().desc("keystore to use for https").build());
		keystorePass = withOpt("keypass").withCli(Option.builder().longOpt("keypass").hasArg().desc("passphrase for the keystore").build());

		vhost = withOpt("vhost").withCli(Option.builder().longOpt("vhost").hasArg().desc("public vhost seen by the servlet, default to the binded address").build());
		keepAlive = withOpt("keepalive")
				.withCli(Option.builder().longOpt("keepalive").hasArg().desc("max time to keep idle keepalive connection in seconds, -1: no keep alive, 0: infinite").build())
				.withValue("2");
	}

	@Override
	protected void process(NIOServer server, CommandLine cli) throws Exception {
		String ks = keystore.value(cli);
		if (ks == null)
			http = http.withValue(":8080");
		else
			https = https.withValue(":8443");

		InetSocketAddress addressHttp = parseAddr(cli, http, "");
		InetSocketAddress addressHttps = parseAddr(cli, https, "");

		String value = cli.getOptionValue(vhost.name());
		if (value == null)
			value = addressHttp == null ? addressHttps.getHostName() : addressHttp.getHostString();

		manager = createServletManager();
		events = createEventManager();
		ctx = createContext(value);

		loadInitializer();
		manager.initialize(ctx, createServlets(), createFilters());
		events.fireContextInitialized(ctx);

		SSLContext sslContext = addressHttps == null ? null : sslContext(ks, keystorePass.value(cli));

		int keepAliveIdle = parseInt(cli, keepAlive, -1) * 1000;
		if (addressHttps != null)
			server.bind(addressHttps, (exec, key, now) -> new NIOConnectionSSL(exec, key, now, new HttpConnection(ctx, manager, events, keepAliveIdle), sslContext));
		if (addressHttp != null)
			server.bind(addressHttp, (exec, key, now) -> new NIOConnectionPlain(exec, key, now, new HttpConnection(ctx, manager, events, keepAliveIdle)));
	}

	private final SSLContext sslContext(String keystore, String password)
			throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
		KeyStore ks = KeyStore.getInstance("JKS");
		try (InputStream is = Files.newInputStream(Paths.get(keystore))) {
			ks.load(is, null);
		}

		KeyManagerFactory keyManager = KeyManagerFactory.getInstance("SunX509");
		keyManager.init(ks, password == null ? null : password.toCharArray());

		TrustManagerFactory trust = TrustManagerFactory.getInstance("SunX509");
		trust.init(ks);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManager.getKeyManagers(), trust.getTrustManagers(), null);
		return sslContext;
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
		NIOServer nioServer = build(arg);
		try {
			nioServer.start();
			nioServer.await();
		} finally {
			nioServer.stop();
			nioServer.await();
			events.fireContextDestroyed();
		}
	}
}
