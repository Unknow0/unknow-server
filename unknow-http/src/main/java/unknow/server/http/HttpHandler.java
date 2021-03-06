/**
 * 
 */
package unknow.server.http;

import java.io.IOException;
import java.io.InputStream;
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

import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.Handler;
import unknow.server.nio.util.Buffers;

public class HttpHandler extends Handler implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(HttpHandler.class);
	private static final Cookie[] COOKIE = new Cookie[0];

	private static final byte[] CRLF = new byte[] { '\r', '\n' };
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

	private static final int METHOD = 0;
	private static final int PATH = 1;
	private static final int QUERY = 2;
	private static final int PROTOCOL = 3;
	private static final int HEADER = 4;
	private static final int CONTENT = 5;

	private final ExecutorService executor;
	private final HttpRawProcessor processor;
	private final int keepAliveIdle;

	private int step = METHOD;
	private int last = 0;
	private int infoStart = -1;
	public final Buffers meta;
	private final StringBuilder sb;
	private final int[] part = new int[4];
	private final int[] headers = new int[255];
	private int headerCount = 0;

	private Future<?> f;

	/**
	 * create new RequestBuilder
	 */
	public HttpHandler(ExecutorService executor, HttpRawProcessor processor, int keepAliveIdle) {
		this.executor = executor;
		this.processor = processor;
		this.keepAliveIdle = keepAliveIdle;
		meta = new Buffers();
		sb = new StringBuilder();
	}

	@Override
	protected void handle(InputStream in, OutputStream out) {
		if (step == CONTENT)
			return;
		if (step == METHOD)
			tryRead(SPACE, MAX_METHOD_SIZE, PATH, HttpError.BAD_REQUEST);
		if (step == PATH) {
			int e = pendingRead.indexOf(SPACE, last, MAX_PATH_SIZE);
			if (e == -2)
				error(HttpError.URI_TOO_LONG);
			if (e < 0)
				return;
			int q = pendingRead.indexOf(QUESTION, last, e - last);
			if (q < 0)
				q = e;
			part[PATH] = q;
			part[QUERY] = e;
			step = PROTOCOL;
			last = e + 1;
		}
		if (step == PROTOCOL)
			tryRead(CRLF, MAX_VERSION_SIZE, HEADER, HttpError.BAD_REQUEST);
		if (step == HEADER) {
			int k;
			while ((k = pendingRead.indexOf(CRLF, last, MAX_HEADER_SIZE)) > 0) {
				if (k == last) {
					pendingRead.read(meta, last);
					pendingRead.skip(2);
					step = CONTENT;
					break;
				}
				headers[headerCount++] = k;
				if (headerCount > headers.length) {
					error(HttpError.BAD_REQUEST);
					return;
				}
				last = k + 2;
			}

			if (k == -2)
				error(HttpError.HEADER_TOO_LARGE);
		}
		if (step == CONTENT)
			f = executor.submit(this);
	}

	private boolean tryRead(byte lookup, int limit, int next, HttpError e) {
		int i = pendingRead.indexOf(lookup, last, limit);
		if (i < 0) {
			if (i == -2)
				error(e);
			return false;
		}
		part[step] = i;
		step = next;
		last = i + 1;
		return true;
	}

	private boolean tryRead(byte[] lookup, int limit, int next, HttpError e) {
		int i = pendingRead.indexOf(lookup, last, limit);
		if (i < 0) {
			if (i == -2)
				error(e);
			return false;
		}
		part[step] = i;
		step = next;
		last = i + lookup.length;
		return true;
	}

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
		sb.setLength(0);
		meta.toString(sb, 0, part[METHOD]);
		return sb.toString();
	}

	/**
	 * @return the servlet path as a string
	 */
	public String parseServletPath() {
		int o = part[METHOD] + 1;
		sb.setLength(0);
		decodePath(sb, meta, o, infoStart < 0 ? part[PATH] : infoStart);
		return sb.toString();
	}

	/**
	 * @return the path info as a string
	 */
	public String parsePathInfo() {
		int e = part[PATH];
		sb.setLength(0);
		decodePath(sb, meta, infoStart < 0 ? e : infoStart, e);
		return sb.toString();
	}

	/**
	 * @return the query string as a string
	 */
	public String parseQuery() {
		sb.setLength(0);
		int o = part[PATH];
		meta.toString(sb, o, part[QUERY] - o);
		return sb.toString();
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
		parseParam(pendingRead, 0, pendingRead.length(), param);
	}

	/**
	 * @return the protocol as a string
	 */
	public String parseProtocol() {
		sb.setLength(0);
		int o = part[QUERY] + 1;
		meta.toString(sb, o, part[PROTOCOL] - o);
		return sb.toString();
	}

	/**
	 * @return the headers as a string
	 */
	public Map<String, List<String>> parseHeader() {
		Map<String, List<String>> map = new HashMap<>();

		int o = part[PROTOCOL] + 2;
		for (int i = 0; i < headerCount; i++) {
			int e = headers[i];
			int indexOf = meta.indexOf(COLON, o, e);
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
			int indexOf = meta.indexOf(COLON, o, e);
			if (indexOf < 0) { // not cookie to parse there
				o = e + 2;
				continue;
			}
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
				next = meta.indexOf(COOKIE_SEP, o, e);
				if (next < 0)
					next = e;
				indexOf = meta.indexOf(EQUAL, o, next);
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
		return cookies.toArray(COOKIE);
	}

	private final void error(HttpError e) {
		log.error("{}: {}", getRemote(), e);
		try {
			getOut().write(e.empty());
			getOut().close();
		} catch (IOException ex) { // OK
		}
	}

	@Override
	public void run() {
		try {
			processor.process(this);
			// TODO prepare for reuse
		} catch (Exception e) {
			try {
				getOut().close();
			} catch (IOException e1) {
				e.addSuppressed(e1);
			}
			log.error("", e);
		}
	}

	@Override
	public boolean isClosed() {
		if (super.isClosed())
			return true;
		if (keepAliveIdle > 0) {
			long e = System.currentTimeMillis() - keepAliveIdle;
			if (lastRead() < e && lastWrite() < e)
				return true;
		}
		// TODO check request timeout
		return false;
	}

	@Override
	public void reset() {
		super.reset();
		meta.clear();
		headerCount = last = 0;
		step = METHOD;
		if (f != null)
			f.cancel(true);
		f = null;
	}

	public int pathStart() {
		return part[METHOD] + 1;
	}

	public int pathEnd() {
		return part[PATH];
	}

	private static ByteBuffer DECODE_BB = ByteBuffer.allocate(10);
	private static CharBuffer DECODE_CB = CharBuffer.allocate(2);
	private static final CharsetDecoder DECODER = StandardCharsets.UTF_8.newDecoder().onUnmappableCharacter(CodingErrorAction.REPLACE).onMalformedInput(CodingErrorAction.REPLACE);

	private static final void decodePath(StringBuilder sb, Buffers b, int o, int e) {
		DECODE_BB.clear();
		DECODE_CB.clear();
		for (;;) {
			int i = b.indexOf(PERCENT, o, e);
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
		while (o < e) {
			int indexOf = in.indexOf(AMPERSAMP, o, e);
			if (indexOf < 0)
				indexOf = e;
			int i = in.indexOf(EQUAL, o, indexOf);
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