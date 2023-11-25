/**
 * 
 */
package unknow.server.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.UnavailableException;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.servlet.ServletResponseImpl;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.ServletManager;
import unknow.server.nio.NIOConnection;
import unknow.server.util.io.Buffers;
import unknow.server.util.io.Buffers.Walker;
import unknow.server.util.io.BuffersUtils;
import unknow.server.util.pool.Pool;

public class HttpConnection extends NIOConnection implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(HttpConnection.class);

	private static final byte[] CRLF = { '\r', '\n' };
	private static final byte[] CRLF2 = { '\r', '\n', '\r', '\n' };
	private static final byte[] PARAM_SEP = { '&', '=' };
	private static final byte[] SPACE_SLASH = { ' ', '/' };
	private static final byte SPACE = ' ';
	private static final byte QUESTION = '?';
	private static final byte COLON = ':';
	private static final byte SEMICOLON = ';';
	private static final byte SLASH = '/';
	private static final byte AMPERSAMP = '&';
	private static final byte PERCENT = '%';
	private static final byte EQUAL = '=';

	private static final int MAX_METHOD_SIZE = 10; // max size for method
	private static final int MAX_PATH_SIZE = 2000;
	private static final int MAX_VERSION_SIZE = 12;
	private static final int MAX_HEADER_SIZE = 512;

	private static final int MAX_START_SIZE = 8192;

	private final ExecutorService executor;
	private final int keepAliveIdle;

	private final ServletContextImpl ctx;
	private final ServletManager servlets;
	private final EventManager events;

	private final StringBuilder sb;
	private final Decode decode;

	private Future<?> f;

	private volatile boolean running = false;

	/**
	 * create new RequestBuilder
	 * @param pool 
	 */
	public HttpConnection(Pool<NIOConnection> pool, ExecutorService executor, ServletContextImpl ctx, int keepAliveIdle) {
		super(pool);
		this.executor = executor;
		this.ctx = ctx;
		this.servlets = ctx.getServletManager();
		this.events = ctx.getEvents();
		this.keepAliveIdle = keepAliveIdle;
		sb = new StringBuilder();
		decode = new Decode(sb);
	}

	@Override
	public final void onRead() throws InterruptedException {
		if (running)
			return;
		int i = BuffersUtils.indexOf(pendingRead, CRLF2, 0, MAX_START_SIZE);
		if (i == -1)
			return;
		if (i == -2) {
			error(HttpError.BAD_REQUEST);
			return;
		}
		running = true;
		f = executor.submit(this);
	}

	@Override
	public final void onWrite() { // OK
	}

	private final void error(HttpError e) {
		try {
			OutputStream out = getOut();
			out.write(e.empty());
			out.close();
		} catch (@SuppressWarnings("unused") IOException ex) { // OK
		}
	}

	private boolean fillRequest(ServletRequestImpl req) throws InterruptedException {
		int i = BuffersUtils.indexOf(pendingRead, SPACE_SLASH, 0, MAX_METHOD_SIZE);
		if (i < 0) {
			error(HttpError.BAD_REQUEST);
			return false;
		}
		BuffersUtils.toString(sb, pendingRead, 0, i);
		req.setMethod(sb.toString());
		sb.setLength(0);
		int last = i + 1;

		i = BuffersUtils.indexOf(pendingRead, SPACE, last, MAX_PATH_SIZE);
		if (i < 0) {
			error(HttpError.URI_TOO_LONG);
			return false;
		}
		int q = BuffersUtils.indexOf(pendingRead, QUESTION, last, i - last);
		if (q < 0)
			q = i;

		pendingRead.walk(decode, last, q - last);
		if (!decode.done())
			return false;
		req.setRequestUri(sb.toString());
		sb.setLength(0);

		int s;
		while ((s = BuffersUtils.indexOf(pendingRead, SLASH, last + 1, q - last - 1)) > 0) {
			int c = BuffersUtils.indexOf(pendingRead, SEMICOLON, last + 1, s - last - 1);
			pendingRead.walk(decode, last + 1, (c < 0 ? s : c) - last - 1);
			if (!decode.done())
				return false;
			req.addPath(sb.toString());
			sb.setLength(0);
			last = s;
		}
		if (s == -2 && last + 1 < q) {
			int c = BuffersUtils.indexOf(pendingRead, SEMICOLON, last + 1, q - last - 1);
			BuffersUtils.toString(sb, pendingRead, last + 1, c < 0 ? q - last - 1 : c);
			req.addPath(sb.toString());
			sb.setLength(0);
		}

		if (q < i) {
			BuffersUtils.toString(sb, pendingRead, q + 1, i - q - 1);
			req.setQuery(sb.toString());
			sb.setLength(0);
		} else
			req.setQuery("");

		Map<String, List<String>> map = new HashMap<>();
		parseParam(map, pendingRead, q + 1, i);
		req.setQueryParam(map);
		last = i + 1;

		i = BuffersUtils.indexOf(pendingRead, CRLF, last, MAX_VERSION_SIZE);
		if (i < 0) {
			error(HttpError.BAD_REQUEST);
			return false;
		}
		BuffersUtils.toString(sb, pendingRead, last, i - last);
		req.setProtocol(sb.toString());
		sb.setLength(0);
		last = i + 2;

		map = new HashMap<>();
		while ((i = BuffersUtils.indexOf(pendingRead, CRLF, last, MAX_HEADER_SIZE)) > last) {
			int c = BuffersUtils.indexOf(pendingRead, COLON, last, i - last);
			if (c < 0) {
				error(HttpError.BAD_REQUEST);
				return false;
			}

			BuffersUtils.toString(sb, pendingRead, last, c - last);
			String k = sb.toString().trim().toLowerCase();
			sb.setLength(0);

			BuffersUtils.toString(sb, pendingRead, c + 1, i - c - 1);
			String v = sb.toString().trim();
			sb.setLength(0);

			List<String> list = map.get(k);
			if (list == null)
				map.put(k, list = new ArrayList<>(1));
			list.add(v);

			last = i + 2;
		}
		req.setHeaders(map);
		pendingRead.skip(last+2);
		return true;
	}

	private boolean parseParam(Map<String, List<String>> map, Buffers data, int o, int e) throws InterruptedException {
		while (o < e) {
			int i = BuffersUtils.indexOfOne(data, PARAM_SEP, o, e - o);
			if (i < 0)
				i = e;
			data.walk(decode, o, i - o);
			if (!decode.done())
				return false;
			String key = sb.toString();
			sb.setLength(0);

			o = i + 1;
			if (i < e && data.get(i) == EQUAL) {
				i = BuffersUtils.indexOf(data, AMPERSAMP, o, e - o);
				if (i < 0)
					i = e;
				data.walk(decode, o, i - o);
				if (!decode.done())
					return false;
				o = i + 1;
			}
			map.computeIfAbsent(key, k -> new ArrayList<>(1)).add(sb.toString());
			sb.setLength(0);
		}
		return true;
	}

	@SuppressWarnings("resource")
	@Override
	public void run() {
		boolean close = false;

		Out out = getOut();
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
			doRun(req, res);
			events.fireRequestDestroyed(req);
			res.close();
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			logger.error("processor error", e);
			error(HttpError.SERVER_ERROR);
		} finally {
			if (close)
				out.close();
			else
				out.flush();
			cleanup();
		}
	}

	private void doRun(ServletRequestImpl req, ServletResponseImpl res) throws IOException {
		FilterChain s = servlets.find(req);
		try {
			s.doFilter(req, res);
		} catch (UnavailableException e) {
			// TODO add page with retry-after
			res.sendError(503, e, null);
		} catch (Exception e) {
			logger.error("failed to service '{}'", s, e);
			if (!res.isCommitted())
				res.sendError(500);
		}

	}

	private void cleanup() {
		running = false;
		pendingRead.clear();
	}

	@Override
	public boolean closed(long now, boolean stop) {
		if (stop)
			return !running;

		if (isClosed())
			return true;
		if (running)
			return false;

		if (keepAliveIdle > 0) {
			long e = now - keepAliveIdle;
			if (lastRead() <= e && lastWrite() <= e)
				return true;
		}

		// TODO check request timeout
		return false;
	}

	@Override
	protected final void onFree() {
		if (running) {
			f.cancel(true);
			running = false;
		}
		cleanup();
	}

	private static final class Decode implements Walker {
		private final CharsetDecoder d = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
		private final char[] tmp = new char[2048];
		private final CharBuffer cbuf = CharBuffer.wrap(tmp);
		private final ByteBuffer bbuf = ByteBuffer.allocate(4096);
		private final StringBuilder sb;

		public Decode(StringBuilder sb) {
			this.sb = sb;
		}

		private int m = 0;
		private byte pending = 0;

		@Override
		public boolean apply(byte[] b, int o, int e) {
			while (o < e) {
				byte c = b[o++];
				if (m > 0) {
					pending = (byte) ((pending << 4) + (c & 0xff) - '0');
					if (--m == 0) {
						bbuf.put(pending);
						pending = 0;
					}
				} else if (c == PERCENT)
					m = 2;
				else
					bbuf.put(c);
				if (bbuf.remaining() == 0)
					decode();

			}
			return false;
		}

		private void decode() {
			bbuf.flip();
			CoderResult r;
			do {
				r = d.decode(bbuf, cbuf, false);
				cbuf.flip();
				int l = cbuf.length();
				cbuf.get(tmp, 0, l);
				sb.append(tmp, 0, l);
				cbuf.clear();
			} while (r.isOverflow());
			bbuf.compact();
		}

		public boolean done() {
			try {
				if (m != 0)
					return false;
				if (bbuf.position() > 0)
					decode();
				return bbuf.position() == 0;
			} finally {
				cbuf.clear();
				bbuf.clear();
			}
		}
	}
}