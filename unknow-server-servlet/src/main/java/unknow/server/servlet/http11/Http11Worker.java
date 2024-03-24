package unknow.server.servlet.http11;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import unknow.server.nio.NIOConnection.Out;
import unknow.server.servlet.Decode;
import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.HttpError;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.impl.in.ChunckedInputStream;
import unknow.server.servlet.impl.in.EmptyInputStream;
import unknow.server.servlet.impl.in.LengthInputStream;
import unknow.server.servlet.impl.out.AbstractServletOutput;
import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersUtils;

public class Http11Worker extends HttpWorker {
	private static final Logger logger = LoggerFactory.getLogger(Http11Worker.class);

	private static final byte[] END = new byte[] { '\r', '\n', '\r', '\n' };

	private static final byte[] CRLF = { '\r', '\n' };
	private static final byte[] PARAM_SEP = { '&', '=' };
	private static final byte[] SPACE_SLASH = { ' ', '/' };
	private static final byte[] QUOTE = new byte[] { '\\', '"' };
	private static final byte[] CHUNKED = new byte[] { 't', 'r', 'a', 'n', 's', 'f', 'e', 'r', '-', 'e', 'n', 'c', 'o', 'd', 'i', 'n', 'g', ':', ' ', 'c', 'h', 'u', 'n', 'k',
			'e', 'd', '\r', '\n' };
	private static final byte[] CONTENT_LENGTH = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 'l', 'e', 'n', 'g', 't', 'h', ':', ' ' };
	private static final byte[] CONTENT_LENGTH0 = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 'l', 'e', 'n', 'g', 't', 'h', ':', ' ', '0', '\r', '\n' };
	private static final byte[] CONTENT_TYPE = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 't', 'y', 'p', 'e', ':', ' ' };
	private static final byte[] CONTENT_HTML = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 't', 'y', 'p', 'e', ':', ' ', 't', 'e', 'x', 't', '/', 'h', 't', 'm', 'l',
			';', 'c', 'h', 'a', 'r', 's', 'e', 't', '=', 'u', 't', 'f', '8', '\r', '\n' };
	private static final byte[] COOKIE = new byte[] { 's', 'e', 't', '-', 'c', 'o', 'o', 'k', 'i', 'e', ':', ' ' };
	private static final byte[] PATH = new byte[] { ';', 'p', 'a', 't', 'h', '=' };
	private static final byte[] DOMAIN = new byte[] { ';', 'd', 'o', 'm', 'a', 'i', 'n', '=' };
	private static final byte[] MAX_AGE = new byte[] { ';', 'm', 'a', 'x', '-', 'a', 'g', 'e' };
	private static final byte[] SECURE = new byte[] { ';', 's', 'e', 'c', 'u', 'r', 'e' };
	private static final byte[] HTTP_ONLY = new byte[] { ';', 'h', 't', 't', 'p', 'o', 'n', 'l', 'y' };
	private static final byte[] ERROR_START = new byte[] { '<', 'h', 't', 'm', 'l', '>', '<', 'b', 'o', 'd', 'y', '>', '<', 'h', '1', '>' };
	private static final byte[] ERROR_END = new byte[] { '<', '/', 'h', '1', '>', '<', '/', 'b', 'o', 'd', 'y', '>', '<', '/', 'h', 't', 'm', 'l', '>' };

	private static final byte SPACE = ' ';
	private static final byte QUESTION = '?';
	private static final byte COLON = ':';
	private static final byte SEMICOLON = ';';
	private static final byte SLASH = '/';
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

	public Http11Worker(HttpConnection co) {
		super(co);
		this.keepAliveIdle = co.getkeepAlive();

		sb = new StringBuilder();
		decode = new Decode(sb);
	}

	@Override
	public InetSocketAddress getRemote() {
		return co.getRemote();
	}

	@Override
	public InetSocketAddress getLocal() {
		return co.getLocal();
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
			writeString(out, res.getContentType());
		}

		@SuppressWarnings("resource")
		AbstractServletOutput rawStream = res.getRawStream();
		if (rawStream instanceof LengthOutputStream) {
			out.write(CONTENT_LENGTH);
			out.write(Long.toString(res.getContentLength()).getBytes(StandardCharsets.US_ASCII));
			out.write(CRLF);
		}
		if (rawStream instanceof ChunckedOutputStream)
			out.write(CHUNKED);
		else
			out.write(CONTENT_LENGTH0);

		for (Cookie c : res.getCookies())
			writeCookie(out, c);
		out.write(CRLF);
	}

	@SuppressWarnings("resource")
	@Override
	public void sendError(HttpError e, Throwable t, String msg) throws IOException {
		Out out = co.getOut();
		if (msg == null) {
			out.write(e.empty());
			return;
		}

		out.write(e.encoded);
		byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);

		out.write(CONTENT_HTML);
		out.write(CONTENT_LENGTH);
		out.write(Integer.toString(bytes.length + ERROR_START.length + ERROR_END.length).getBytes(StandardCharsets.US_ASCII));
		out.write(CRLF);
		out.write(CRLF);
		out.write(ERROR_START);
		out.write(bytes);
		out.write(ERROR_END);
	}

	@SuppressWarnings("resource")
	@Override
	public final void doStart() throws IOException, InterruptedException {
		boolean close = false;

		Out out = co.getOut();
		if (!fillRequest(req))
			return;

		if ("100-continue".equals(req.getHeader("expect"))) {
			out.write(HttpError.CONTINUE.encoded);
			out.write('\r');
			out.write('\n');
			out.flush();
		}

		close = keepAliveIdle == 0 || !"keep-alive".equals(req.getHeader("connection"));
		if (!close)
			res.setHeader("connection", "keep-alive");
	}

	@Override
	protected void doDone() {
	}

	private boolean fillRequest(ServletRequestImpl req) throws InterruptedException, IOException {
		Buffers b = co.pendingRead;
		int i = BuffersUtils.indexOf(b, SPACE_SLASH, 0, MAX_METHOD_SIZE);
		if (i < 0) {
			res.sendError(HttpError.BAD_REQUEST.code, null, null);
			return false;
		}
		BuffersUtils.toString(sb, b, 0, i);
		req.setMethod(sb.toString());
		sb.setLength(0);
		int last = i + 1;

		i = BuffersUtils.indexOf(b, SPACE, last, MAX_PATH_SIZE);
		if (i < 0) {
			res.sendError(HttpError.URI_TOO_LONG.code, null, null);
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

		int s;
		while ((s = BuffersUtils.indexOf(b, SLASH, last + 1, q - last - 1)) > 0) {
			int c = BuffersUtils.indexOf(b, SEMICOLON, last + 1, s - last - 1);
			b.walk(decode, last + 1, (c < 0 ? s : c) - last - 1);
			if (!decode.done())
				return false;
			req.addPath(sb.toString());
			sb.setLength(0);
			last = s;
		}
		if (s == -2 && last + 1 < q) {
			int c = BuffersUtils.indexOf(b, SEMICOLON, last + 1, q - last - 1);
			BuffersUtils.toString(sb, b, last + 1, c < 0 ? q - last - 1 : c);
			req.addPath(sb.toString());
			sb.setLength(0);
		}

		if (q < i) {
			BuffersUtils.toString(sb, b, q + 1, i - q - 1);
			req.setQuery(sb.toString());
			sb.setLength(0);
		} else
			req.setQuery("");

		Map<String, List<String>> map = new HashMap<>();
		parseParam(map, b, q + 1, i);
		req.setQueryParam(map);
		last = i + 1;

		i = BuffersUtils.indexOf(b, CRLF, last, MAX_VERSION_SIZE);
		if (i < 0) {
			res.sendError(HttpError.BAD_REQUEST.code, null, null);
			return false;
		}
		BuffersUtils.toString(sb, b, last, i - last);
		req.setProtocol(sb.toString());
		sb.setLength(0);
		last = i + 2;

		map = new HashMap<>();
		while ((i = BuffersUtils.indexOf(b, CRLF, last, MAX_HEADER_SIZE)) > last) {
			int c = BuffersUtils.indexOf(b, COLON, last, i - last);
			if (c < 0) {
				res.sendError(HttpError.BAD_REQUEST.code, null, null);
				return false;
			}

			BuffersUtils.toString(sb, b, last, c - last);
			String k = sb.toString().trim().toLowerCase();
			sb.setLength(0);

			BuffersUtils.toString(sb, b, c + 1, i - c - 1);
			String v = sb.toString().trim();
			sb.setLength(0);

			List<String> list = map.get(k);
			if (list == null)
				map.put(k, list = new ArrayList<>(1));
			list.add(v);

			last = i + 2;
		}
		req.setHeaders(map);
		b.skip(last + 2);
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