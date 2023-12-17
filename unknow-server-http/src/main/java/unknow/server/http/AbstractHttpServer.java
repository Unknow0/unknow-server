package unknow.server.http;

import java.net.InetSocketAddress;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletException;
import unknow.server.http.servlet.FilterConfigImpl;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.ServletManager;
import unknow.server.nio.NIOServer;
import unknow.server.nio.NIOServerBuilder;

/**
 * Abstract server
 * 
 * @author unknow
 */
public abstract class AbstractHttpServer extends NIOServerBuilder {
	private Opt addr;
	private Opt vhost;
	private Opt execMin;
	private Opt execMax;
	private Opt execIdle;
	private Opt keepAlive;

	protected ServletContextImpl ctx;
	protected ServletManager servlets;
	protected EventManager events;

	protected abstract ServletManager createServletManager();

	protected abstract EventManager createEventManager();

	protected abstract ServletContextImpl createContext(String vhost);

	protected abstract ServletConfigImpl[] createServlets();

	protected abstract FilterConfigImpl[] createFilters();

	@Override
	protected void beforeParse() {
		addr = withOpt("addr").withCli(Option.builder("a").longOpt("addr").hasArg().desc("address to bind to").build()).withValue(":8080");
		vhost = withOpt("vhost").withCli(Option.builder().longOpt("vhost").desc("public vhost seen by the servlet, default to the binded address").build());
		execMin = withOpt("exec-min").withCli(Option.builder().longOpt("exec-min").desc("min number of exec thread to use").build()).withValue("0");
		execMax = withOpt("exec-max").withCli(Option.builder().longOpt("exec-max").desc("max number of exec thread to use").build())
				.withValue(Integer.toString(Integer.MAX_VALUE));
		execIdle = withOpt("exec-idle").withCli(Option.builder().longOpt("exec-idle").desc("max idle time for exec thread in seconds").build()).withValue("60");
		keepAlive = withOpt("exec-idle")
				.withCli(Option.builder().longOpt("exec-idle").desc("max time to keep idle keepalive connection, -1: infinite, 0: no keep alive").build()).withValue("2000");
	}

	@Override
	protected void process(NIOServer server, CommandLine cli) throws Exception {
		InetSocketAddress address = parseAddr(cli, addr, "");
		String value = cli.getOptionValue(vhost.name());
		if (value == null)
			value = address.getHostString();

		servlets = createServletManager();
		events = createEventManager();
		ctx = createContext(value);

		loadInitializer();
		servlets.initialize(ctx, createServlets(), createFilters());
		events.fireContextInitialized(ctx);

		AtomicInteger i = new AtomicInteger();
		ExecutorService executor = new ThreadPoolExecutor(parseInt(cli, execMin, 0), parseInt(cli, execMax, 0), parseInt(cli, execIdle, 0), TimeUnit.SECONDS,
				new SynchronousQueue<>(), r -> {
					Thread t = new Thread(r, "exec-" + i.getAndIncrement());
					t.setDaemon(true);
					return t;
				});
		int keepAliveIdle = parseInt(cli, keepAlive, -1);
		server.bind(address, () -> new HttpConnection(executor, ctx, keepAliveIdle));
	}

	protected void loadInitializer() throws ServletException {
		for (ServletContainerInitializer i : ServiceLoader.load(ServletContainerInitializer.class)) {
			i.onStartup(null, ctx);
		}
	}

	public void process(String[] arg) throws Exception {
		NIOServer nioServer = build(arg);
		try {
			nioServer.start();
			nioServer.await();
		} finally {
			nioServer.stop();
			nioServer.await();
			events.fireContextDestroyed(ctx);
		}
	}
}
