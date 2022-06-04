/**
 * 
 */
package unknow.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
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
import unknow.server.nio.Handler;
import unknow.server.nio.util.Buffers;

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

	private static final int METHOD = 0;
	private static final int PATH = 1;
	private static final int QUERY = 2;
	private static final int PROTOCOL = 3;
	private static final int HEADER = 4;
	private static final int CONTENT = 5;

	private final Connection co;

	private final ExecutorService executor;
	private final int keepAliveIdle;

	private final ServletContextImpl ctx;
	private final ServletManager servlets;
	private final EventManager events;

	private int step = METHOD;
	private int last = 0;
	private int infoStart = -1;
	public final Buffers meta;
	private final StringBuilder sb;
	private final int[] part = new int[4];
	private final int[] headers = new int[255];
	private int headerCount = 0;

//	private String method;
//	private String path;
//	private String version;

	private volatile Future<?> f;

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
	public void onRead() {
		if (f != null) {
			synchronized (co.pendingRead) {
				co.pendingRead.notifyAll();
			}
			log.info("> notify {}", co);
			return;
		}
		int i = co.pendingRead.indexOf(CRLF2, MAX_START_SIZE);
		if (i == -1)
			return;
		if (i == -2) {
			error(HttpError.BAD_REQUEST);
			return;
		}
		co.pendingRead.read(meta, i + 2);
		co.pendingRead.skip(2);
		f = executor.submit(this);

//		if (step == METHOD)
//			tryRead(SPACE, MAX_METHOD_SIZE, PATH, HttpError.BAD_REQUEST);
//		if (step == PATH) {
//			int e = pendingRead.indexOf(SPACE, last, MAX_PATH_SIZE);
//			if (e == -2)
//				error(HttpError.URI_TOO_LONG);
//			if (e < 0)
//				return;
//			int q = pendingRead.indexOf(QUESTION, last, e - last);
//			if (q < 0)
//				q = e;
//			part[PATH] = q;
//			part[QUERY] = e;
//			step = PROTOCOL;
//			last = e + 1;
//		}
//		if (step == PROTOCOL)
//			tryRead(CRLF, MAX_VERSION_SIZE, HEADER, HttpError.BAD_REQUEST);
//		if (step == HEADER) {
//			int k;
//			while ((k = pendingRead.indexOf(CRLF, last, MAX_HEADER_SIZE)) > 0) {
//				if (k == last) {
//					pendingRead.read(meta, last);
//					pendingRead.skip(2);
//					step = CONTENT;
//					break;
//				}
//				headers[headerCount++] = k;
//				if (headerCount > headers.length) {
//					error(HttpError.BAD_REQUEST);
//					return;
//				}
//				last = k + 2;
//			}
//
//			if (k == -2)
//				error(HttpError.HEADER_TOO_LARGE);
//		}
//		if (step == CONTENT && f == null)
	}

	@Override
	public void onWrite() {
	}

//	private boolean tryRead(byte lookup, int limit, int next, HttpError e) {
//		int i = meta.indexOf(lookup, last, limit);
//		if (i < 0) {
//			if (i == -2)
//				error(e);
//			return null;
//		}
//		part[step] = i;
//		step = next;
//		last = i + 1;
//		return true;
//	}
//
//	private boolean tryRead(byte[] lookup, int limit, int next, HttpError e) {
//		int i = meta.indexOf(lookup, last, limit);
//		if (i < 0) {
//			if (i == -2)
//				error(e);
//			return false;
//		}
//		part[step] = i;
//		step = next;
//		last = i + lookup.length;
//		return true;
//	}

	/**
	 * set path start index of path info
	 * 
	 * @param infoStart
	 */
	public final void setPathInfoStart(int infoStart) {
		this.infoStart = infoStart;
	}

	/**
	 * @return the http method as a string
	 */
	public String parseMethod() {
		synchronized (sb) {
			sb.setLength(0);
			meta.toString(sb, 0, part[METHOD]);
			return sb.toString();
		}
	}

	/**
	 * @return the servlet path as a string
	 */
	public String parseServletPath() {
		int o = part[METHOD] + 1;
		synchronized (sb) {
			sb.setLength(0);
			decodePath(sb, meta, o, infoStart < 0 ? part[PATH] : infoStart);
			return sb.toString();
		}
	}

	/**
	 * @return the path info as a string
	 */
	public String parsePathInfo() {
		int e = part[PATH];
		synchronized (sb) {
			sb.setLength(0);
			decodePath(sb, meta, infoStart < 0 ? e : infoStart, e);
			return sb.toString();
		}
	}

	/**
	 * @return the query string as a string
	 */
	public String parseQuery() {
		synchronized (sb) {
			sb.setLength(0);
			int o = part[PATH] + 1;
			int l = part[QUERY] - o;
			if (l > 0)
				meta.toString(sb, o, l);
			return sb.toString();
		}
	}

	/**
	 * parse the query as parameters
	 * 
	 * @param param the output param
	 */
	public void parseQueryParam(Map<String, List<String>> param) {
		parseParam(meta, part[PATH] + 1, part[QUERY], param);
	}

	/**
	 * parse the content as parameters
	 * 
	 * @param param the output param
	 */
	public void parseContentParam(Map<String, List<String>> param) {
//		parseParam(pendingRead, 0, pendingRead.length(), param);
	}

	/**
	 * @return the protocol as a string
	 */
	public String parseProtocol() {
		synchronized (sb) {
			sb.setLength(0);
			int o = part[QUERY] + 1;
			meta.toString(sb, o, part[PROTOCOL] - o);
			return sb.toString();
		}
	}

	/**
	 * @return the headers as a string
	 */
	public Map<String, List<String>> parseHeader() {
		Map<String, List<String>> map = new HashMap<>();

		synchronized (sb) {
			int o = part[PROTOCOL] + 2;
			for (int i = 0; i < headerCount; i++) {
				int e = headers[i];
				int indexOf = meta.indexOf(COLON, o, e - o);
				if (indexOf < 0)
					indexOf = e;
				sb.setLength(0);
				meta.toString(sb, o, indexOf - o);
				String k = sb.toString().toLowerCase();
				List<String> list = map.get(k);
				if (list == null)
					map.put(k, list = new ArrayList<>());
				if (indexOf < e && !parseHeaderValue(list, indexOf + 1, e)) {
					list.clear();
					sb.setLength(0);
					meta.toString(sb, indexOf + 1, e - indexOf - 1);
					list.add(sb.toString().trim());
				}
				o = e + 2;
			}
		}
		return map;
	}

	/**
	 * parse a header value
	 * 
	 * @param list the output value
	 * @param o    the starting offset
	 * @param e    the end offset
	 * @return false if the parsing failed
	 */
	private boolean parseHeaderValue(List<String> list, int o, int e) {

		for (;;) {
			sb.setLength(0);
			o = meta.indexOfNot(WS, o, e - o);
			if (o < 0)
				return true;
			if (meta.get(o) == QUOTE) {
				o++;
				int i = meta.indexOf(QUOTE, o, e - o);
				if (i < 0)
					return false;
				while (meta.get(i - 1) == '\\') {
					meta.toString(sb, o, i - o);
					o = i + 1;
					i = meta.indexOf(QUOTE, o, e - o);
					if (i == -1)
						return false;
				}
				meta.toString(sb, o, i - o);
				o = i + 1;
			} else {
				int i = meta.indexOfOne(SEP, o, e - o);
				if (i < 0) { // we are done
					meta.toString(sb, o, e - o);
					list.add(sb.toString().trim());
					return true;
				}
				meta.toString(sb, o, i - o);
				o = i;
			}
			int j = meta.indexOf(COMA, o, e - o);
			if (j != meta.indexOfNot(WS, o, e - o))
				return false;
			if (sb.length() > 0)
				list.add(sb.toString());
			if (j < 0)
				return true;
			o = j + 1;
		}
	}

	/**
	 * @return the cookies
	 */
	public Cookie[] parseCookie() {
		List<Cookie> cookies = new ArrayList<>();
		int o = part[PROTOCOL] + 2;
		for (int i = 0; i < headerCount; i++) {
			int e = headers[i];
			int indexOf = meta.indexOf(COLON, o, e - o);
			if (indexOf < 0) { // not cookie to parse there
				o = e + 2;
				continue;
			}
			synchronized (sb) {
				sb.setLength(0);
				meta.toString(sb, o, indexOf - o);
				if (!"cookie".equals(sb.toString().toLowerCase())) {
					o = e + 2;
					continue;
				}
				o = meta.indexOfNot(WS, indexOf + 1, e);
				if (o == -1) {
					o = e + 2;
					continue;
				}
				// parse cookie
				int next;
				do {
					next = meta.indexOf(COOKIE_SEP, o, e - o);
					if (next < 0)
						next = e;
					indexOf = meta.indexOf(EQUAL, o, next - o);
					if (indexOf < 0)
						break;
					sb.setLength(0);
					meta.toString(sb, o, indexOf - o);
					String n = sb.toString();
					sb.setLength(0);
					meta.toString(sb, indexOf + 1, next - indexOf - 1);
					cookies.add(new Cookie(n, sb.toString()));
					o = next + 2;
				} while (next < e);
				o = e + 2;
			}
		}
		return cookies.toArray(COOKIE);
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

	private boolean fillRequest(ServletRequestImpl req) {
		int i = meta.indexOf(SPACE, 0, MAX_METHOD_SIZE);
		if (i < 0) {
			error(HttpError.BAD_REQUEST);
			return false;
		}
		meta.toString(sb, 0, i);
		req.setMethod(sb.toString());
		sb.setLength(0);
		int last = i + 1;

		i = meta.indexOf(SPACE, last, MAX_PATH_SIZE);
		if (i < 0) {
			error(HttpError.URI_TOO_LONG);
			return false;
		}
		int q = meta.indexOf(QUESTION, last, i - last);
		if (q < 0)
			q = i;

		meta.toString(sb, last, q - last);
		String path = sb.toString();
		sb.setLength(0);

		part[METHOD] = last - 1;
		part[PATH] = q;

		meta.toString(sb, q, i - q);
		req.setQuery(sb.toString());
		sb.setLength(0);
		last = i + 1;

		i = meta.indexOf(CRLF, last, MAX_VERSION_SIZE);
		if (i < 0) {
			error(HttpError.BAD_REQUEST);
			return false;
		}
		meta.toString(sb, last, i - last);
		req.setProtocol(sb.toString());
		sb.setLength(0);
		last = i + 2;

		Map<String, List<String>> map = new HashMap<>();
		while ((i = meta.indexOf(CRLF, last, MAX_HEADER_SIZE)) > 0) {
			int c = meta.indexOf(COLON, last, i - last);
			if (c < 0) {
				error(HttpError.BAD_REQUEST);
				return false;
			}

			meta.toString(sb, last, c - last);
			String k = sb.toString().trim().toLowerCase();
			sb.setLength(0);

			meta.toString(sb, c + 1, i - c - 1);
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

		OutputStream out = co.getOut();
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
			close = keepAliveIdle == 0 || !"keep-alive".equalsIgnoreCase(req.getHeader("connection"));
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
			String connection = res.getHeader("connection");
			if (!close && connection != null && !"keep-alive".equals(connection))
				close = true;
			res.close();
		} catch (Exception e) {
			log.error("processor error", e);
		} finally {
			cleanup();
//			if (close)
			try {
				out.close();
			} catch (IOException e) { // OK
			}
		}
	}

	private void cleanup() {
		f = null;
		meta.clear();
		headerCount = last = 0;
		step = METHOD;
	}

	@Override
	public boolean closed(boolean stop) {
		if (stop)
			return f == null;

		if (co.isClosed())
			return true;
		if (f != null)
			return false;
		if (keepAliveIdle >= 0) {
			long e = System.currentTimeMillis() - keepAliveIdle;
			if (co.lastRead() < e && co.lastWrite() < e)
				return true;
		}
		// TODO check request timeout
		return false;
	}

	@Override
	public void free() {
		reset();
	}

	@Override
	public void reset() {
		if (f != null)
			f.cancel(true);
		cleanup();
	}

	public int pathStart() {
		return part[METHOD] + 1;
	}

	public int pathEnd() {
		return part[PATH];
	}

	private ByteBuffer DECODE_BB = ByteBuffer.allocate(10);
	private CharBuffer DECODE_CB = CharBuffer.allocate(2);
	private static final CharsetDecoder DECODER = StandardCharsets.UTF_8.newDecoder().onUnmappableCharacter(CodingErrorAction.REPLACE)
			.onMalformedInput(CodingErrorAction.REPLACE);

	private final void decodePath(StringBuilder sb, Buffers b, int o, int e) {
		DECODE_BB.clear();
		DECODE_CB.clear();
		for (;;) {
			int i = b.indexOf(PERCENT, o, e - o);
			if (i != o && DECODE_BB.position() > 0) {
				DECODE_BB.flip();
				CoderResult r = DECODER.decode(DECODE_BB, DECODE_CB, true);
				while (r.isOverflow()) {
					CharBuffer tmp = CharBuffer.allocate(DECODE_CB.capacity() * 2);
					DECODE_CB.flip();
					tmp.put(DECODE_CB);
					DECODE_CB = tmp;
					r = DECODER.decode(DECODE_BB, DECODE_CB, true);
				}
				sb.append(DECODE_CB.array(), 0, DECODE_CB.position());
				DECODE_BB.compact();
			}
			if (i >= 0) {
				b.toString(sb, o, i++ - o);
				if (DECODE_BB.remaining() == 0) {
					ByteBuffer tmp = ByteBuffer.allocate(DECODE_BB.capacity() * 2);
					DECODE_BB.flip();
					tmp.put(DECODE_BB);
					DECODE_BB = tmp;
				}
				DECODE_BB.put((byte) (Character.digit(b.get(i++), 16) * 16 + Character.digit(b.get(i++), 16)));
				o = i;
			} else {
				b.toString(sb, o, e - o);
				break;
			}
		}
	}

	private void parseParam(Buffers in, int o, int e, Map<String, List<String>> param) {
		synchronized (sb) {
			while (o < e) {
				int indexOf = in.indexOf(AMPERSAMP, o, e - o);
				if (indexOf < 0)
					indexOf = e;
				int i = in.indexOf(EQUAL, o, indexOf - o);
				String n;
				String v;
				if (i > 0) {
					sb.setLength(0);
					decodePath(sb, in, o, i);
					n = sb.toString();
					sb.setLength(0);
					decodePath(sb, in, i + 1, indexOf);
					v = sb.toString();
				} else {
					sb.setLength(0);
					decodePath(sb, in, o, indexOf);
					n = sb.toString();
					v = "";
				}
				List<String> list = param.get(n);
				if (list == null)
					param.put(n, list = new ArrayList<>());
				list.add(v);
				o = indexOf + 1;
			}
		}
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