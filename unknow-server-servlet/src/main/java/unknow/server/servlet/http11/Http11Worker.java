package unknow.server.servlet.http11;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import unknow.server.nio.NIOConnection.Out;
import unknow.server.servlet.Decode;
import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.HttpError;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.AbstractServletOutput;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.impl.ServletResponseImpl;
import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersUtils;

/** http/1.1 worker */
public final class Http11Worker extends HttpWorker {
	private static final Logger logger = LoggerFactory.getLogger(Http11Worker.class);

	private static final byte[] CRLF = { '\r', '\n' };
	private static final byte[] PARAM_SEP = { '&', '=' };
	private static final byte[] SPACE_SLASH = { ' ', '/' };
	private static final byte[] QUOTE = new byte[] { '\\', '"' };
	private static final byte[] CHUNKED = new byte[] { 't', 'r', 'a', 'n', 's', 'f', 'e', 'r', '-', 'e', 'n', 'c', 'o', 'd', 'i', 'n', 'g', ':', ' ', 'c', 'h', 'u', 'n', 'k',
			'e', 'd', '\r', '\n' };
	private static final byte[] CONTENT_LENGTH = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 'l', 'e', 'n', 'g', 't', 'h', ':', ' ' };
	private static final byte[] CONTENT_LENGTH0 = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 'l', 'e', 'n', 'g', 't', 'h', ':', ' ', '0', '\r', '\n' };
	private static final byte[] CONTENT_TYPE = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 't', 'y', 'p', 'e', ':', ' ' };
	private static final byte[] COOKIE = new byte[] { 's', 'e', 't', '-', 'c', 'o', 'o', 'k', 'i', 'e', ':', ' ' };
	private static final byte[] PATH = new byte[] { ';', 'p', 'a', 't', 'h', '=' };
	private static final byte[] DOMAIN = new byte[] { ';', 'd', 'o', 'm', 'a', 'i', 'n', '=' };
	private static final byte[] MAX_AGE = new byte[] { ';', 'm', 'a', 'x', '-', 'a', 'g', 'e' };
	private static final byte[] SECURE = new byte[] { ';', 's', 'e', 'c', 'u', 'r', 'e' };
	private static final byte[] HTTP_ONLY = new byte[] { ';', 'h', 't', 't', 'p', 'o', 'n', 'l', 'y' };

	private static final byte SPACE = ' ';
	private static final byte QUESTION = '?';
	private static final byte COLON = ':';
	private static final byte AMPERSAMP = '&';
	private static final byte EQUAL = '=';

	private static final String UNKNOWN = "Unknown";

	private static final int MAX_METHOD_SIZE = 10; // max size for method
	private static final int MAX_PATH_SIZE = 2000;
	private static final int MAX_VERSION_SIZE = 12;
	private static final int MAX_HEADER_SIZE = 512;

	private final int keepAliveIdle;
	private final StringBuilder sb;
	private final Decode decode;

	/**
	 * new worker
	 * @param co the connection
	 */
	public Http11Worker(HttpConnection co) {
		super(co);
		this.keepAliveIdle = co.getkeepAlive();

		sb = new StringBuilder();
		decode = new Decode(sb);
	}

	@SuppressWarnings("resource")
	@Override
	public ServletInputStream createInput() {
		String tr = req.getHeader("transfer-encoding");
		if ("chunked".equalsIgnoreCase(tr))
			return new ChunckedInputStream(co.getIn());
		long l = req.getContentLengthLong();
		if (l > 0)
			return new LengthInputStream(co.getIn(), l);
		return EmptyInputStream.INSTANCE;
	}

	@SuppressWarnings("resource")
	@Override
	public AbstractServletOutput createOutput() {
		long contentLength = res.getContentLength();
		if (contentLength < 0)
			return new ChunckedOutputStream(co.getOut(), res);
		if (contentLength == 0)
			return EmptyStream.INSTANCE;
		return new LengthOutputStream(co.getOut(), res, contentLength);
	}

	@Override
	public void commit() throws IOException {
		HttpError http = HttpError.fromStatus(res.getStatus());
		@SuppressWarnings("resource")
		Out out = co.getOut();
		out.write(http == null ? HttpError.encodeStatusLine(res.getStatus(), UNKNOWN) : http.encoded);

		for (String s : res.getHeaderNames())
			writeHeader(out, s, res.getHeaders(s));

		if (res.getHeader("content-type") == null && res.getContentType() != null) {
			out.write(CONTENT_TYPE);
			out.write(res.getContentType().getBytes(StandardCharsets.US_ASCII));
			out.write(CRLF);
		}

		@SuppressWarnings("resource")
		AbstractServletOutput rawStream = res.getRawStream();
		if (rawStream instanceof LengthOutputStream) {
			out.write(CONTENT_LENGTH);
			out.write(Long.toString(res.getContentLength()).getBytes(StandardCharsets.US_ASCII));
			out.write(CRLF);
		} else if (rawStream instanceof ChunckedOutputStream) {
			out.write(CHUNKED);
		} else
			out.write(CONTENT_LENGTH0);

		for (Cookie c : res.getCookies())
			writeCookie(out, c);
		out.write(CRLF);
	}

	@Override
	public void run() {
		super.run();
		while (!co.pendingRead().isEmpty()) {
			this.req = new ServletRequestImpl(this, DispatcherType.REQUEST);
			this.res = new ServletResponseImpl(this);
			super.run();
		}
	}

	@SuppressWarnings("resource")
	@Override
	public final boolean doStart() throws IOException, InterruptedException {
		if (!fillRequest(req)) {
			logger.warn("Failed to process request");
			return false;
		}

		if ("100-continue".equals(req.getHeader("expect"))) {
			Out out = co.getOut();
			out.write(HttpError.CONTINUE.encoded);
			out.write('\r');
			out.write('\n');
			out.flush();
		}

		String header = req.getHeader("connection");
		if (keepAliveIdle != 0 && (header == null || "keep-alive".equalsIgnoreCase(header))) {
			res.setHeader("connection", "keep-alive");
			res.setHeader("keep-alive", "timeout=" + (keepAliveIdle / 1000));
		} else
			res.setHeader("connection", "close");
		return true;
	}

	@Override
	protected void doDone() {
		if (!"keep-alive".equalsIgnoreCase(res.getHeader("connection")))
			co.getOut().close();
	}

	private boolean fillRequest(ServletRequestImpl req) throws InterruptedException, IOException {
		Buffers b = co.pendingRead();
		int i = BuffersUtils.indexOf(b, SPACE_SLASH, 0, MAX_METHOD_SIZE);
		if (i < 0) {
			sendError(HttpError.BAD_REQUEST.code, null, null);
			return false;
		}
		BuffersUtils.toString(sb, b, 0, i);
		req.setMethod(sb.toString());
		sb.setLength(0);
		int last = i + 1;

		i = BuffersUtils.indexOf(b, SPACE, last, MAX_PATH_SIZE);
		if (i < 0) {
			sendError(HttpError.URI_TOO_LONG.code, null, null);
			return false;
		}
		int q = BuffersUtils.indexOf(b, QUESTION, last, i - last);
		if (q < 0)
			q = i;

		b.walk(decode, last, q - last);
		if (!decode.done())
			return false;
		req.setRequestUri(sb.toString());
		sb.setLength(0);

		if (q < i) {
			BuffersUtils.toString(sb, b, q + 1, i - q - 1);
			req.setQuery(sb.toString());
			sb.setLength(0);
		} else
			req.setQuery("");

		parseParam(req.getQueryParam(), b, q + 1, i);
		last = i + 1;

		i = BuffersUtils.indexOf(b, CRLF, last, MAX_VERSION_SIZE);
		if (i < 0) {
			sendError(HttpError.BAD_REQUEST.code, null, null);
			return false;
		}
		BuffersUtils.toString(sb, b, last, i - last);
		req.setProtocol(sb.toString());
		sb.setLength(0);
		last = i + 2;

		while ((i = BuffersUtils.indexOf(b, CRLF, last, MAX_HEADER_SIZE)) > last) {
			int c = BuffersUtils.indexOf(b, COLON, last, i - last);
			if (c < 0) {
				sendError(HttpError.BAD_REQUEST.code, null, null);
				return false;
			}

			BuffersUtils.toString(sb, b, last, c - last);
			String k = sb.toString().trim().toLowerCase();
			sb.setLength(0);

			BuffersUtils.toString(sb, b, c + 1, i - c - 1);
			String v = sb.toString().trim();
			sb.setLength(0);

			req.addHeader(k, v);
			last = i + 2;
		}
		b.skip(last + 2L);
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

	/**
	 * check if this string contain a special characters as a header value
	 * 
	 * @param s the string to check
	 * @return true is we should quote in the response
	 */
	private static boolean shouldEscape(String s) {
		int l = s.length();
		for (int i = 0; i < l; i++) {
			char c = s.charAt(i);
			if (c == ' ' || c == '(' || c == ')' || c == '<' || c == '>' || c == '@' || c == ',' || c == ';' || c == ':' || c == '\\' || c == '"' || c == '/' || c == '['
					|| c == ']' || c == '?' || c == '=' || c == '{' || c == '}')
				return true;
		}
		return false;
	}

	private static void writeHeader(Out out, String name, Collection<String> values) throws IOException {
		out.write(name.getBytes(StandardCharsets.US_ASCII));
		out.write(':');
		for (String s : values) {
			out.write(' ');
			writeString(out, s);
		}
		out.write(CRLF);
	}

	private static void writeCookie(Out out, Cookie c) throws IOException {
		out.write(COOKIE);
		out.write(c.getName().getBytes(StandardCharsets.US_ASCII));
		out.write('=');
		out.write(c.getValue().getBytes(StandardCharsets.US_ASCII));
		if (c.getPath() != null) {
			out.write(PATH);
			writeString(out, c.getPath());
		}
		if (c.getDomain() != null) {
			out.write(DOMAIN);
			writeString(out, c.getDomain());
		}
		if (c.getMaxAge() > 0) {
			out.write(MAX_AGE);
			writeString(out, Integer.toString(c.getMaxAge()));
		}
		if (c.getSecure())
			out.write(SECURE);
		if (c.isHttpOnly())
			out.write(HTTP_ONLY);
	}

	private static void writeString(Out out, String s) throws IOException {
		boolean escape = shouldEscape(s);
		if (escape)
			out.write('"');
		int l = 0;
		int i;
		while ((i = s.indexOf('"', l)) != -1) {
			out.write(s.substring(l, i).getBytes(StandardCharsets.US_ASCII));
			out.write(QUOTE);
			l = i + 1;
		}
		out.write(s.substring(l).getBytes(StandardCharsets.US_ASCII));
		if (escape)
			out.write('"');
	}
}
