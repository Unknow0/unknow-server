package unknow.server.http;

import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import picocli.CommandLine.Option;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.utils.EmptyQueue;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.ServletManager;
import unknow.server.nio.cli.NIOServerCli;

public abstract class AbstractHttpServer extends NIOServerCli {

	/**
	 * public vhost seen be the servlet (default to the binded address)
	 */
	@Option(names = "--vhost", description = "public vhost seen be the servlet (default to the binded address)", descriptionKey = "vhost")
	public String vhost;

	/**
	 * min number of execution thread to use, default to 0
	 */
	@Option(names = "--exec-min", description = "min number of exec thread to use, default to 0", descriptionKey = "exec-min")
	public int execMin = 0;

	/**
	 * max number of execution thread to use, default to Integer.MAX_VALUE
	 */
	@Option(names = "--exec-max", description = "max number of exec thread to use, default to Integer.MAX_VALUE", descriptionKey = "exec-max")
	public int execMax = Integer.MAX_VALUE;

	/**
	 * max idle time for exec thread, default to 60
	 */
	@Option(names = "--exec-idle", description = "max idle time for exec thread, default to 60", descriptionKey = "exec-idle")
	public long execIdle = 600L;

	/**
	 * max time to keep idle keepalive connection, default to -1
	 */
	@Option(names = "--keep-alive-idle", description = "max time to keep idle keepalive connection, -1: infinite, 0: no keep alive,  default to -1", descriptionKey = "keep-alive-idle")
	public int keepAliveIdle = -1;

	protected final ServletContextImpl ctx;
	protected final ServletManager servlets;
	protected final EventManager events;

	protected AbstractHttpServer() {
		servlets = createServletManager();
		events = createEventManager();
		ctx = createContext();
	}

	protected abstract ServletManager createServletManager();

	protected abstract EventManager createEventManager();

	protected abstract ServletContextImpl createContext();

	protected abstract void initialize() throws ServletException;

	private final void loadInitializer() throws ServletException {
		for (ServletContainerInitializer i : ServiceLoader.load(ServletContainerInitializer.class)) {
			i.onStartup(null, ctx);
		}
	}

	@Override()
	public final Integer call() throws Exception {
		AtomicInteger i = new AtomicInteger();
		ExecutorService executor = new ThreadPoolExecutor(execMin, execMax, execIdle, TimeUnit.SECONDS, new EmptyQueue<Runnable>(), r -> {
			Thread t = new Thread(r, "exec-" + i.getAndIncrement());
			t.setDaemon(true);
			return t;
		});

		handler = co -> new HttpHandler(co, executor, ctx, keepAliveIdle);

		loadInitializer();
		initialize();
		ctx.getEvents().fireContextInitialized(ctx);
		Integer err = super.call();
		ctx.getEvents().fireContextDestroyed(ctx);
		return err;
	}
}
