/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import unknow.server.http.HttpConnection;
import unknow.server.http.HttpError;
import unknow.server.http.servlet.out.ChunckedOutputStream;
import unknow.server.http.servlet.out.EmptyStream;
import unknow.server.http.servlet.out.LengthOutputStream;
import unknow.server.http.servlet.out.Output;
import unknow.server.http.servlet.out.ServletWriter;
import unknow.server.nio.NIOConnection.Out;

/**
 * @author unknow
 */
public class ServletResponseImpl implements HttpServletResponse {
	private static final Logger logger = LoggerFactory.getLogger(ServletResponseImpl.class);

	private static final String UNKNOWN = "Unknown";

	private static final byte[] CRLF = new byte[] { '\r', '\n' };
	private static final byte[] QUOTE = new byte[] { '\\', '"' };
	private static final byte[] CHUNKED = new byte[] { 't', 'r', 'a', 'n', 's', 'f', 'e', 'r', '-', 'e', 'n', 'c', 'o', 'd', 'i', 'n', 'g', ':', ' ', 'c', 'h', 'u', 'n', 'k',
			'e', 'd', '\r', '\n' };
	private static final byte[] CONTENT_LENGTH = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 'l', 'e', 'n', 'g', 't', 'h', ':', ' ' };
	private static final byte[] CONTENT_LENGTH0 = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 'l', 'e', 'n', 'g', 't', 'h', ':', ' ', '0', '\r', '\n' };
	private static final byte[] CONTENT_TYPE = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 't', 'y', 'p', 'e', ':', ' ' };
	private static final byte[] CONTENT_HTML = new byte[] { 'c', 'o', 'n', 't', 'e', 'n', 't', '-', 't', 'y', 'p', 'e', ':', ' ', 't', 'e', 'x', 't', '/', 'h', 't', 'm', 'l',
			';', 'c', 'h', 'a', 'r', 's', 'e', 't', '=', 'u', 't', 'f', '8', '\r', '\n' };
	private static final byte[] CHARSET = new byte[] { ';', 'c', 'h', 'a', 'r', 's', 'e', 't', '=' };
	private static final byte[] LOCATION = new byte[] { 'l', 'o', 'c', 'a', 't', 'i', 'o', 'n', ':', ' ' };
	private static final byte[] COOKIE = new byte[] { 's', 'e', 't', '-', 'c', 'o', 'o', 'k', 'i', 'e', ':', ' ' };
	private static final byte[] PATH = new byte[] { ';', 'p', 'a', 't', 'h', '=' };
	private static final byte[] DOMAIN = new byte[] { ';', 'd', 'o', 'm', 'a', 'i', 'n', '=' };
	private static final byte[] MAX_AGE = new byte[] { ';', 'm', 'a', 'x', '-', 'a', 'g', 'e' };
	private static final byte[] SECURE = new byte[] { ';', 's', 'e', 'c', 'u', 'r', 'e' };
	private static final byte[] HTTP_ONLY = new byte[] { ';', 'h', 't', 't', 'p', 'o', 'n', 'l', 'y' };
	private static final byte[] ERROR_START = new byte[] { '<', 'h', 't', 'm', 'l', '>', '<', 'b', 'o', 'd', 'y', '>', '<', 'h', '1', '>' };
	private static final byte[] ERROR_END = new byte[] { '<', '/', 'h', '1', '>', '<', '/', 'b', 'o', 'd', 'y', '>', '<', '/', 'h', 't', 'm', 'l', '>' };

	private static final DateTimeFormatter RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

	private final HttpConnection p;
	private Output servletOut;

	private boolean commited = false;

	private int status;
	private Charset charset;
	private String type;
	private final Map<String, List<String>> headers;
	private final List<Cookie> cookies;

	private long contentLength = -1;
	private int bufferSize = -1;

	// Content-Language
	private Locale locale = null;

	/**
	 * create new ServletResponseImpl
	 * 
	 * @param p the connection
	 */
	public ServletResponseImpl(HttpConnection p) {
		this.p = p;
		headers = new HashMap<>();
		cookies = new ArrayList<>();

		status = 200;
	}

	/**
	 * commit the response
	 * 
	 * @throws IOException
	 */
	public final void commit() throws IOException {
		if (commited)
			return;
		commited = true;

		HttpError http = HttpError.fromStatus(status);
		Out out = p.getOut();
		out.write(http == null ? HttpError.encodeStatusLine(status, UNKNOWN) : http.encoded);
		for (Entry<String, List<String>> e : headers.entrySet())
			writeHeader(e.getKey(), e.getValue());
		if (type != null && !headers.containsKey("content-type")) {
			out.write(CONTENT_TYPE);
			out.write(type.getBytes(StandardCharsets.US_ASCII));
			if (charset != null) {
				out.write(CHARSET);
				out.write(charset.name().getBytes(StandardCharsets.US_ASCII));
			}
			out.write(CRLF);
		}
		if (servletOut != null && servletOut.isChuncked())
			out.write(CHUNKED);
		else if (servletOut == null || contentLength == 0)
			out.write(CONTENT_LENGTH0);
		else {
			out.write(CONTENT_LENGTH);
			out.write(Long.toString(contentLength).getBytes(StandardCharsets.US_ASCII));
			out.write(CRLF);
		}
		for (Cookie c : cookies)
			writeCookie(c);
		out.write(CRLF);
	}

	public void writeHeader(String name, List<String> values) throws IOException {
		Out out = p.getOut();
		out.write(name.getBytes(StandardCharsets.US_ASCII));
		out.write(':');
		for (String s : values) {
			out.write(' ');
			writeString(s);
		}
		out.write(CRLF);
	}

	public void writeCookie(Cookie c) throws IOException {
		Out out = p.getOut();
		out.write(COOKIE);
		out.write(c.getName().getBytes(StandardCharsets.US_ASCII));
		out.write('=');
		out.write(c.getValue().getBytes(StandardCharsets.US_ASCII));
		if (c.getPath() != null) {
			out.write(PATH);
			writeString(c.getPath());
		}
		if (c.getDomain() != null) {
			out.write(DOMAIN);
			writeString(c.getDomain());
		}
		if (c.getMaxAge() > 0) {
			out.write(MAX_AGE);
			writeString(Integer.toString(c.getMaxAge()));
		}
		if (c.getSecure())
			out.write(SECURE);
		if (c.isHttpOnly())
			out.write(HTTP_ONLY);
	}

	public void writeString(String s) throws IOException {
		boolean escape = shouldEscape(s);
		Out out = p.getOut();
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

	public void close() throws IOException {
		if (servletOut != null)
			servletOut.close();
		commit();
	}

	public void checkCommited() {
		if (commited)
			throw new IllegalStateException("already commited");
	}

	public void sendError(HttpError e, int sc, String msg) throws IOException {
		Out out = p.getOut();
		if (msg == null) {
			out.write(e == null ? HttpError.encodeEmptyReponse(sc, UNKNOWN) : e.empty());
			return;
		}

		out.write(e == null ? HttpError.encodeStatusLine(sc, UNKNOWN) : e.encoded);
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

	@SuppressWarnings({ "unchecked", "resource" })
	private <T extends ServletOutputStream & Output> T createStream() {
		if (contentLength < 0)
			return (T) new ChunckedOutputStream(p.getOut(), this, bufferSize);
		if (contentLength == 0)
			return (T) EmptyStream.INSTANCE;
		return (T) new LengthOutputStream(p.getOut(), this, contentLength);
	}

	@Override
	public String getCharacterEncoding() {
		return charset == null ? p.getCtx().getResponseCharacterEncoding() : charset.name();
	}

	@Override
	public void setCharacterEncoding(String charset) {
		this.charset = Charset.forName(charset);
	}

	@Override
	public void setContentType(String type) {
		int i = type.indexOf("charset=");
		if (i > 0) {
			String encoding = type.substring(i + 8);
			i = encoding.indexOf(' ');
			if (i > 0)
				encoding = encoding.substring(0, i);
			i = encoding.indexOf(';');
			if (i > 0)
				encoding = encoding.substring(0, i);
			setCharacterEncoding(encoding);
		}
		this.type = type;
	}

	@Override
	public String getContentType() {
		if (!type.contains("charset="))
			type += "; charset=" + getCharacterEncoding();
		return type;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (servletOut == null)
			servletOut = createStream();
		if (servletOut instanceof ServletOutputStream)
			return (ServletOutputStream) servletOut;
		throw new IllegalStateException("output already got created with getWriter()");
	}

	@SuppressWarnings({ "rawtypes", "unchecked", "resource" })
	@Override
	public PrintWriter getWriter() throws IOException {
		if (servletOut == null)
			servletOut = new ServletWriter(createStream(), charset == null ? Charset.forName(p.getCtx().getResponseCharacterEncoding()) : charset);
		if (servletOut instanceof Writer)
			return new PrintWriter((Writer) servletOut);
		throw new IllegalStateException("output already got created with getWriter()");
	}

	@Override
	public void setContentLength(int len) {
		contentLength = len;
	}

	@Override
	public void setContentLengthLong(long len) {
		contentLength = len;
	}

	@Override
	public void setBufferSize(int size) {
		if (servletOut != null)
			servletOut.setBufferSize(size);
		else
			bufferSize = size;
	}

	@Override
	public int getBufferSize() {
		return servletOut == null ? bufferSize : servletOut.getBufferSize();
	}

	@Override
	public void flushBuffer() throws IOException {
		commit();
		if (servletOut != null)
			servletOut.flush();
		p.getOut().flush();
	}

	@Override
	public void resetBuffer() {
		checkCommited();
		if (servletOut != null)
			servletOut.resetBuffers();
	}

	@Override
	public void reset() {
		resetBuffer();
		status = 200;
		List<String> list = headers.get("connection");
		headers.clear();
		if (list != null)
			headers.put("connection", list);
		servletOut = null;
	}

	@Override
	public final boolean isCommitted() {
		return commited;
	}

	@Override
	public void setLocale(Locale loc) {
		if (charset == null)
			setCharacterEncoding(p.getCtx().getEncoding(loc));
		this.locale = loc;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public void addCookie(Cookie cookie) {
		cookies.add(cookie);
	}

	@Override
	public String encodeURL(String url) {
		return url;
	}

	@Override
	public String encodeRedirectURL(String url) {
		return url;
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		p.sendError(sc, null, msg);
	}

	@Override
	public void sendError(int sc) throws IOException {
		p.sendError(sc, null, null);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		checkCommited();
		commited = true;
		status = HttpError.FOUND.code;
		commit();
		Out out = p.getOut();
		out.write(HttpError.FOUND.encoded);
		out.write(CONTENT_LENGTH0);
		out.write(LOCATION);
		writeString(location);
		out.write(CRLF);
	}

	@Override
	public String getHeader(String name) {
		List<String> list = headers.get(name.toLowerCase());
		return list == null ? null : list.get(0);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		return headers.get(name.toLowerCase());
	}

	@Override
	public Collection<String> getHeaderNames() {
		return headers.keySet();
	}

	@Override
	public boolean containsHeader(String name) {
		return headers.containsKey(name.toLowerCase());
	}

	@Override
	public void setHeader(String name, String value) {
		name = name.toLowerCase();
		List<String> list = headers.get(name);
		if (list == null)
			headers.put(name, list = new ArrayList<>(1));
		else
			list.clear();
		list.add(value);
	}

	@Override
	public void addHeader(String name, String value) {
		headers.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>(1)).add(value);
	}

	@Override
	public void setDateHeader(String name, long date) {
		setHeader(name, RFC1123.format(Instant.ofEpochMilli(date)));
	}

	@Override
	public void addDateHeader(String name, long date) {
		addHeader(name, RFC1123.format(Instant.ofEpochMilli(date)));
	}

	@Override
	public void setIntHeader(String name, int value) {
		setHeader(name, Integer.toString(value));
	}

	@Override
	public void addIntHeader(String name, int value) {
		addHeader(name, Integer.toString(value));
	}

	@Override
	public void setStatus(int sc) {
		this.status = sc;
	}

	@Override
	public int getStatus() {
		return status;
	}

	/**
	 * check if this string contain a special characters as a header value
	 * 
	 * @param s the string to check
	 * @return true is we should quote in the response
	 */
	public static boolean shouldEscape(String s) {
		int l = s.length();
		for (int i = 0; i < l; i++) {
			char c = s.charAt(i);
			if (c == ' ' || c == '(' || c == ')' || c == '<' || c == '>' || c == '@' || c == ',' || c == ';' || c == ':' || c == '\\' || c == '"' || c == '/' || c == '['
					|| c == ']' || c == '?' || c == '=' || c == '{' || c == '}')
				return true;
		}
		return false;
	}
}
