package unknow.server.http.test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.FilterChain;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import unknow.server.http.HttpHandler;
import unknow.server.http.HttpRawProcessor;
import unknow.server.http.HttpRawRequest;
import unknow.server.http.PathMatcher.EndMatcher;
import unknow.server.http.PathMatcher.ExactMatcher;
import unknow.server.http.PathMatcher.StartMatcher;
import unknow.server.http.servlet.ArrayMap;
import unknow.server.http.servlet.EventManager;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletManager;
import unknow.server.http.servlet.ServletManager.SEntry;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.nio.Handler;
import unknow.server.nio.HandlerFactory;
import unknow.server.nio.cli.NIOServerCli;

final class Server extends NIOServerCli implements HttpRawProcessor {

	Logger log = LoggerFactory.getLogger(Server.class);

	private static final byte[] NOT_FOUND = new byte[] { 72, 84, 84, 80, 47, 49, 46, 49, 32, 52, 48, 52, 32, 78, 111, 116, 32, 70, 111, 117, 110, 100, 13, 10, 67, 111, 110, 116, 101, 110, 116, 45, 76, 101, 110, 103, 116, 104, 58, 32, 48, 13, 10, 13, 10 };

	private static final byte[] ERROR = new byte[] { 72, 84, 84, 80, 47, 49, 46, 49, 32, 53, 48, 48, 32, 83, 101, 114, 118, 101, 114, 32, 69, 114, 114, 111, 114, 13, 10, 67, 111, 110, 116, 101, 110, 116, 45, 76, 101, 110, 103, 116, 104, 58, 32, 48, 13, 10, 13, 10 };

	private final ServletManager SERVLETS;

	private final EventManager EVENTS;

	private final ServletContextImpl CTX;

	private Server() {
		EVENTS = this.createEventManager();
		SERVLETS = this.createServletManager();
		CTX = this.createContext();
	}

	private final ServletManager createServletManager() {
		javax.servlet.Servlet[] s = new javax.servlet.Servlet[] { new Servlet() };
		return new ServletManager(s, new SEntry[] { new SEntry(new StartMatcher(new byte[] { 47, 116, 101, 115, 116 }), s[0]) });
	}

	private final EventManager createEventManager() {
		return new EventManager(new ArrayList<>(0), new ArrayList<>(0), new ArrayList<>(Arrays.asList((Servlet) SERVLETS.getServlets()[0])), new ArrayList<>(0), new ArrayList<>(0), new ArrayList<>(0), new ArrayList<>(0));
	}

	private final ServletContextImpl createContext() {
		ArrayMap<String> initParam = new ArrayMap<>();
		return new ServletContextImpl("ROOT", initParam, SERVLETS, EVENTS);
	}

	private final void loadInitializer() throws ServletException {
		for (ServletContainerInitializer i : ServiceLoader.load(ServletContainerInitializer.class)) {
			i.onStartup(null, CTX);
		}
	}

	private final void initialize() throws ServletException {
		javax.servlet.Servlet[] s = SERVLETS.getServlets();
		s[0].init(new ServletConfigImpl("test", CTX, new ArrayMap<>(new String[] { "content" }, new String[] { "it works" })));
	}

	@Override()
	public final void process(HttpRawRequest request, OutputStream out) throws IOException {
		ServletRequestImpl req = new ServletRequestImpl(CTX, request);
		EVENTS.fireRequestInitialized(req);
		FilterChain s = SERVLETS.find(req);
		if (s == null) {
			out.write(NOT_FOUND);
			out.close();
			EVENTS.fireRequestDestroyed(req);
			return;
		}
		ServletResponse res = null;
		try {
			s.doFilter(req, res);
		} catch (Exception e) {
			log.error("failed to service '{}'", e, s);
			out.write(ERROR);
		}
		EVENTS.fireRequestDestroyed(req);
		out.close();
	}

	@Override()
	public final Integer call() throws Exception {
		loadInitializer();
		initialize();
		EVENTS.fireContextInitialized(CTX);
		Integer err = super.call();
		EVENTS.fireContextDestroyed(CTX);
		return err;
	}

	public static void main(String[] arg) {
		Server c = new Server();
		ExecutorService executor = Executors.newCachedThreadPool(r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
		c.handler = new HandlerFactory() {

			@Override()
			protected final Handler create() {
				return new HttpHandler(executor, c);
			}
		};
		System.exit(new CommandLine(c).execute(arg));
	}
}
