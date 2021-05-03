package unknow.server.http.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import unknow.server.http.HttpHandler;
import unknow.server.http.HttpRawProcessor;
import unknow.server.http.servlet.DefaultServlet;
import unknow.server.http.servlet.FilterChainImpl;
import unknow.server.http.servlet.FilterChainImpl.ChangePath;
import unknow.server.http.servlet.FilterChainImpl.ServletFilter;
import unknow.server.http.servlet.FilterConfigImpl;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.servlet.ServletResponseImpl;
import unknow.server.http.servlet.session.NoSessionFactory;
import unknow.server.http.utils.ArrayMap;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.IntArrayMap;
import unknow.server.http.utils.ObjectArrayMap;
import unknow.server.http.utils.PathTree;
import unknow.server.http.utils.PathTree.EndNode;
import unknow.server.http.utils.Resource;
import unknow.server.http.utils.ServletManager;
import unknow.server.nio.Handler;
import unknow.server.nio.HandlerFactory;
import unknow.server.nio.cli.NIOServerCli;

final class Server extends NIOServerCli implements HttpRawProcessor {

	private static final Logger log = LoggerFactory.getLogger(Server.class);

	/**
	 * min number of execution thread to use, default to 0
	 */
	@Option(names = "--exec-min", description = "min number of exec thread to use, default to 0", descriptionKey = "exec-min")
	int execMin = 0;

	/**
	 * max number of execution thread to use, default to Integer.MAX_VALUE
	 */
	@Option(names = "--exec-max", description = "max number of exec thread to use, default to Integer.MAX_VALUE", descriptionKey = "exec-max")
	int execMax = Integer.MAX_VALUE;

	/**
	 * max idle time for exec thread, default to 60
	 */
	@Option(names = "--exec-idle", description = "max idle time for exec thread, default to 60", descriptionKey = "exec-idle")
	long execIdle = 60L;

	private final ServletManager SERVLETS;

	private final EventManager EVENTS;

	private final ServletContextImpl CTX;

	private Server() {
		SERVLETS = this.createServletManager();
		EVENTS = this.createEventManager();
		CTX = this.createContext();
	}

	private final EventManager createEventManager() {
		return new EventManager(new ArrayList<>(0), new ArrayList<>(0), new ArrayList<>(Arrays.asList((Servlet) SERVLETS.getServlets()[0])), new ArrayList<>(0), new ArrayList<>(0), new ArrayList<>(0), new ArrayList<>(0));
	}

	private final ServletManager createServletManager() {
		Servlet s0 = new Servlet();
		DefaultServlet s1 = new DefaultServlet(new ArrayMap<>(new String[] { "/404.html" }, new Resource[] { new Resource(1618069378466L, 106L) }));
		FilterChain cs0 = new ServletFilter(s0);
		FilterChain cs1 = new ServletFilter(s1);
		FilterChain cs0s0 = new FilterChainImpl(s0, cs0);
		FilterChain cs0s1 = new FilterChainImpl(s0, cs1);
		return new ServletManager(new javax.servlet.Servlet[] { s0, s1 }, new Filter[] { s0 }, new PathTree(null, new PathTree[] { new PathTree(new byte[] { 116, 101, 115, 116 }, null, null, cs0s0, cs0), new PathTree(new byte[] { 98, 108, 97 }, new PathTree[] { new PathTree(new byte[] { 121, 101, 115 }, null, null, null, cs0) }, null, cs0, cs1), new PathTree(new byte[] { 102, 111, 111 }, null, null, cs0s1, cs0s1), new PathTree(new byte[] { 52, 48, 52 }, null, null, cs0, cs1) }, new EndNode[] { new EndNode(new byte[] { 46, 116, 101, 115, 116 }, cs0) }, null, cs1), new IntArrayMap<>(new int[] { 404 }, new FilterChain[] { new ChangePath("/404.html", cs1) }), new ObjectArrayMap<>(new Class[] {}, new FilterChain[] {}, (a, b) -> a.getName().compareTo(b.getName())));
	}

	private final ServletContextImpl createContext() {
		return new ServletContextImpl("test", new ArrayMap<>(new String[] { "ctx" }, new String[] { "value" }), SERVLETS, EVENTS, new NoSessionFactory(), new ArrayMap<>(new String[] { "fr-FR" }, new String[] { "utf8" }), new ArrayMap<>(new String[] { "7z", "aac", "avi", "bmp", "bz", "bz2", "css", "csv", "gif", "htm", "html", "ico", "ics", "jar", "jpeg", "jpg", "js", "json", "jsp", "mid", "midi", "mpeg", "oga", "ogv", "ogx", "otf", "pdf", "png", "rar", "rtf", "sh", "svg", "tar", "tif", "tiff", "ts", "ttf", "wav", "weba", "webm", "webp", "woff", "woff2", "xhtml", "xml", "zip" }, new String[] { "application/x-7z-compressed", "audio/aac", "video/x-msvideo", "image/bmp", "application/x-bzip", "application/x-bzip2", "text/css", "text/csc", "image/gif", "text/html", "text/html", "image/x-icon", "text/calendar", "application/java-archive", "image/jpeg", "image/jpeg", "application/javascript", "application/json", "text/html", "audio/midi", "audio/midi", "video/mpeg", "audio/ogg", "video/ogg", "application/ogg", "font/otf", "application/pdf", "image/png", "application/x-rar-compressed", "application/rtf", "application/x-sh", "image/svg+xml", "application/x-tar", "image/tiff", "image/tiff", "application/typescript", "font/ttf", "audio/x-wav", "audio/webm", "video/webm", "image/webp", "font/woff", "font/woff2", "application/xhtml+xml", "application/xml", "application/zip" }));
	}

	private final void loadInitializer() throws ServletException {
		for (ServletContainerInitializer i : ServiceLoader.load(ServletContainerInitializer.class)) {
			i.onStartup(null, CTX);
		}
	}

	private final void initialize() throws ServletException {
		javax.servlet.Servlet[] s = SERVLETS.getServlets();
		s[1].init(new ServletConfigImpl("default", CTX, new ArrayMap<>(new String[] {}, new String[] {})));
		s[0].init(new ServletConfigImpl("test", CTX, new ArrayMap<>(new String[] { "content" }, new String[] { "it works" })));
		Filter[] f = SERVLETS.getFilters();
		f[0].init(new FilterConfigImpl("test", CTX, new ArrayMap<>(new String[] { "filter key" }, new String[] { "the value" })));
	}

	@Override()
	public final void process(HttpHandler request) throws IOException {
		ServletResponseImpl res = new ServletResponseImpl(CTX, request.getOut(), request);
		ServletRequestImpl req = new ServletRequestImpl(CTX, request, DispatcherType.REQUEST, res);
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
		if (res.getHeader("connection") == null && req.getHeader("connection") != null)
			res.setHeader("connection", req.getHeader("connection"));
		res.close();
	}

	@Override()
	public final Integer call() throws Exception {
		ExecutorService executor = new ThreadPoolExecutor(execMin, execMax, execIdle, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
		handler = new HandlerFactory() {

			@Override()
			protected final Handler create() {
				return new HttpHandler(executor, Server.this);
			}
		};
		loadInitializer();
		initialize();
		EVENTS.fireContextInitialized(CTX);
		Integer err = super.call();
		EVENTS.fireContextDestroyed(CTX);
		return err;
	}

	public static void main(String[] arg) {
		System.exit(new CommandLine(new Server()).execute(arg));
	}
}
