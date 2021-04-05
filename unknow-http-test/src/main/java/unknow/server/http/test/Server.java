package unknow.server.http.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import unknow.server.http.HttpHandler;
import unknow.server.http.HttpRawProcessor;
import unknow.server.http.servlet.FilterChainImpl;
import unknow.server.http.servlet.FilterChainImpl.ServletFilter;
import unknow.server.http.servlet.FilterConfigImpl;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.servlet.ServletResponseImpl;
import unknow.server.http.utils.ArrayMap;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.IntArrayMap;
import unknow.server.http.utils.ObjectArrayMap;
import unknow.server.http.utils.PathTree;
import unknow.server.http.utils.PathTree.EndNode;
import unknow.server.http.utils.ServletManager;
import unknow.server.nio.Handler;
import unknow.server.nio.HandlerFactory;
import unknow.server.nio.cli.NIOServerCli;

final class Server extends NIOServerCli implements HttpRawProcessor {

	private static final Logger log = LoggerFactory.getLogger(unknow.server.http.test.Server.class);

	private final ServletManager SERVLETS;

	private final EventManager EVENTS;

	private final ServletContextImpl CTX;

	private Server() {
		SERVLETS = this.createServletManager();
		EVENTS = this.createEventManager();
		CTX = this.createContext();
	}

	private final EventManager createEventManager() {
		return new EventManager(new ArrayList<>(0), new ArrayList<>(0), new ArrayList<>(Arrays.asList((unknow.server.http.test.Servlet) SERVLETS.getServlets()[0])), new ArrayList<>(0), new ArrayList<>(0), new ArrayList<>(0), new ArrayList<>(0));
	}

	private final ServletManager createServletManager() {
		unknow.server.http.test.Servlet s0 = new unknow.server.http.test.Servlet();
		ServletFilter cs0 = new ServletFilter(s0);
		FilterChain[] acs0cs0cs0cs0cs0 = new FilterChain[] { cs0, cs0, cs0, cs0, cs0 };
		FilterChainImpl cs0s0 = new FilterChainImpl(s0, cs0);
		FilterChain[] acs0cs0cs0s0cs0cs0s0 = new FilterChain[] { cs0, cs0, cs0s0, cs0, cs0s0 };
		return new ServletManager(new javax.servlet.Servlet[] { s0 }, new Filter[] { s0 }, new PathTree(null, new PathTree[] { new PathTree(new byte[] { 116, 101, 115, 116 }, null, null, acs0cs0cs0s0cs0cs0s0, acs0cs0cs0cs0cs0), new PathTree(new byte[] { 98, 108, 97 }, new PathTree[] { new PathTree(new byte[] { 121, 101, 115 }, null, null, null, acs0cs0cs0cs0cs0) }, null, acs0cs0cs0cs0cs0, null), new PathTree(new byte[] { 102, 111, 111 }, null, null, null, null) }, new EndNode[] { new EndNode(new byte[] { 46, 116, 101, 115, 116 }, acs0cs0cs0cs0cs0) }, null, null), new IntArrayMap<>(new int[] {}, new List[] {}), new ObjectArrayMap<>(new Class[] {}, new List[] {}, (a, b) -> a.getName().compareTo(b.getName())));
	}

	private final ServletContextImpl createContext() {
		return new ServletContextImpl("test", new ArrayMap<>(new String[] { "ctx" }, new String[] { "value" }), SERVLETS, EVENTS);
	}

	private final void loadInitializer() throws ServletException {
		for (ServletContainerInitializer i : ServiceLoader.load(ServletContainerInitializer.class)) {
			i.onStartup(null, CTX);
		}
	}

	private final void initialize() throws ServletException {
		javax.servlet.Servlet[] s = SERVLETS.getServlets();
		s[0].init(new ServletConfigImpl("test", CTX, new ArrayMap<>(new String[] { "content" }, new String[] { "it works" })));
		Filter[] f = SERVLETS.getFilters();
		f[0].init(new FilterConfigImpl("test", CTX, new ArrayMap<>(new String[] { "filter key" }, new String[] { "the value" })));
	}

	@Override()
	public final void process(HttpHandler request) throws IOException {
		ServletRequestImpl req = new ServletRequestImpl(CTX, request, DispatcherType.REQUEST);
		ServletResponseImpl res = new ServletResponseImpl(CTX, request.getOut(), req);
		EVENTS.fireRequestInitialized(req);
		FilterChain s = SERVLETS.find(req);
		if (s != null)
			try {
				s.doFilter(req, res);
			} catch (Exception e) {
				log.error("failed to service '{}'", s, e);
				if (!res.isCommited())
					res.sendError(500, null);
			}
		else
			res.sendError(404, null);
		EVENTS.fireRequestDestroyed(req);
		res.close();
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
		unknow.server.http.test.Server c = new unknow.server.http.test.Server();
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
