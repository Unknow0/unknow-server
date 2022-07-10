/**
 * 
 */
package unknow.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.servlet.ServletResponseImpl;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.ServletManager;
import unknow.server.nio.Connection;
import unknow.server.nio.Connection.Out;
import unknow.server.nio.Handler;
import unknow.server.nio.util.Buffers;
import unknow.server.nio.util.BuffersUtils;

public class HttpHandler implements Handler, Runnable {
	private static final Logger log = LoggerFactory.getLogger(HttpHandler.class);
	private static final Cookie[] COOKIE = new Cookie[0];

	private static final byte[] CRLF = new byte[] { '\r', '\n' };
	private static final byte[] CRLF2 = new byte[] { '\r', '\n', '\r', '\n' };
	private static final byte[] WS = new byte[] { ' ', '\t' };
	private static final byte[] SEP = new byte[] { ' ', '\t', '"', ',' };
	private static final byte[] COOKIE_SEP = new byte[] { ';', ' ' };
	private static final byte SPACE = ' ';
	private static final byte QUESTION = '?';
	private static final byte COLON = ':';
	private static final byte SLASH = '/';
	private static final byte AMPERSAMP = '&';
	private static final byte PERCENT = '%';
	private static final byte QUOTE = '"';
	private static final byte COMA = ',';
	private static final byte EQUAL = '=';

	private static final int MAX_METHOD_SIZE = 10; // max size for method
	private static final int MAX_PATH_SIZE = 2000;
	private static final int MAX_VERSION_SIZE = 12;
	private static final int MAX_HEADER_SIZE = 512;

	private static final int MAX_START_SIZE = 8192;

	private final Connection co;

	private final ExecutorService executor;
	private final int keepAliveIdle;

	private final ServletContextImpl ctx;
	private final ServletManager servlets;
	private final EventManager events;

	public final Buffers meta;
	private final StringBuilder sb;

	private Future<?> f;

	private volatile boolean running = false;

	/**
	 * create new RequestBuilder
	 */
	public HttpHandler(Connection co, ExecutorService executor, ServletContextImpl ctx, int keepAliveIdle) {
		this.co = co;
		this.executor = executor;
		this.ctx = ctx;
		this.servlets = ctx.getServletManager();
		this.events = ctx.getEvents();
		this.keepAliveIdle = keepAliveIdle;
		meta = new Buffers();
		sb = new StringBuilder();
	}

	@Override
	public final void onRead() throws InterruptedException {
		if (running)
			return;
		int i = BuffersUtils.indexOf(co.pendingRead, CRLF2, 0, MAX_START_SIZE);
		if (i == -1)
			return;
		if (i == -2) {
			error(HttpError.BAD_REQUEST);
			return;
		}
		co.pendingRead.read(meta, i + 2, false);
		co.pendingRead.skip(2);
		running = true;
		f = executor.submit(this);
	}

	@Override
	public final void onWrite() {
	}

	private final void error(HttpError e) {
		log.error("{}: {}", co.getRemote(), e);
		try {
			OutputStream out = co.getOut();
			out.write(e.empty());
			out.close();
		} catch (IOException ex) { // OK
		}
	}

	private boolean fillRequest(ServletRequestImpl req) throws InterruptedException {
		int i = BuffersUtils.indexOf(meta, SPACE, 0, MAX_METHOD_SIZE);
		if (i < 0) {
			error(HttpError.BAD_REQUEST);
			return false;
		}
		BuffersUtils.toString(sb, meta, 0, i);
		req.setMethod(sb.toString());
		sb.setLength(0);
		int last = i + 1;

		i = BuffersUtils.indexOf(meta, SPACE, last, MAX_PATH_SIZE);
		if (i < 0) {
			error(HttpError.URI_TOO_LONG);
			return false;
		}
		int q = BuffersUtils.indexOf(meta, QUESTION, last, i - last);
		if (q < 0)
			q = i;

		int s = last;
		while ((s = BuffersUtils.indexOf(meta, SLASH, s + 1, q - s - 1)) > 0) {
			BuffersUtils.toString(sb, meta, last + 1, s - last - 1);
			req.addPath(sb.toString());
			sb.setLength(0);
			last = s;
		}
		if (s == -2) {
			BuffersUtils.toString(sb, meta, last + 1, q - last - 1);
			req.addPath(sb.toString());
			sb.setLength(0);
		}

		BuffersUtils.toString(sb, meta, q, i - q);
		req.setQuery(sb.toString());
		sb.setLength(0);
		last = i + 1;

		i = BuffersUtils.indexOf(meta, CRLF, last, MAX_VERSION_SIZE);
		if (i < 0) {
			error(HttpError.BAD_REQUEST);
			return false;
		}
		BuffersUtils.toString(sb, meta, last, i - last);
		req.setProtocol(sb.toString());
		sb.setLength(0);
		last = i + 2;

		Map<String, List<String>> map = new HashMap<>();
		while ((i = BuffersUtils.indexOf(meta, CRLF, last, MAX_HEADER_SIZE)) > 0) {
			int c = BuffersUtils.indexOf(meta, COLON, last, i - last);
			if (c < 0) {
				error(HttpError.BAD_REQUEST);
				return false;
			}

			BuffersUtils.toString(sb, meta, last, c - last);
			String k = sb.toString().trim().toLowerCase();
			sb.setLength(0);

			BuffersUtils.toString(sb, meta, c + 1, i - c - 1);
			String v = sb.toString().trim();
			sb.setLength(0);

			List<String> list = map.get(k);
			if (list == null)
				map.put(k, list = new ArrayList<>(1));
			list.add(v);

			last = i + 2;
		}
		req.setHeaders(map);
		return true;
	}

	@Override
	public void run() {
		boolean close = false;

		Out out = co.getOut();
		try {
			ServletResponseImpl res = new ServletResponseImpl(ctx, out, this);
			ServletRequestImpl req = new ServletRequestImpl(ctx, this, DispatcherType.REQUEST, res);

			if (!fillRequest(req))
				return;

			if ("100-continue".equals(req.getHeader("expect"))) {
				out.write(HttpError.CONTINUE.encoded);
				out.write(CRLF);
				out.flush();
			}
			close = keepAliveIdle == 0 || !"keep-alive".equals(req.getHeader("connection"));
			if (!close)
				res.setHeader("connection", "keep-alive");
			events.fireRequestInitialized(req);
			FilterChain s = servlets.find(req);
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
			events.fireRequestDestroyed(req);
			res.close();
		} catch (Exception e) {
			log.error("processor error", e);
			error(HttpError.SERVER_ERROR);
		} finally {
			cleanup();
			if (close)
				out.close();
			else
				out.flush();
		}
	}

	private void cleanup() {
		running = false;
		try {
			meta.clear();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public boolean closed(long now, boolean stop) {
		if (stop)
			return !running;

		if (co.isClosed())
			return true;
		if (running)
			return false;

		if (keepAliveIdle > 0) {
			long e = now - keepAliveIdle;
			if (co.lastRead() <= e && co.lastWrite() <= e)
				return true;
		}
		try {
			if (!co.pendingRead.isEmpty()) {
				onRead();
				return false;
			}
		} catch (InterruptedException e) {
		}

		// TODO check request timeout
		return false;
	}

	@Override
	public void free() {
		if (running) {
			f.cancel(true);
			running = false;
		}
		cleanup();
	}

	/**
	 * @return
	 */
	public InetSocketAddress getLocal() {
		return co.getLocal();
	}

	/**
	 * @return
	 */
	public InetSocketAddress getRemote() {
		return co.getRemote();
	}

	/**
	 * @return
	 */
	public InputStream getIn() {
		return co.getIn();
	}
}