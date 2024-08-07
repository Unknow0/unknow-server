/**
 * 
 */
package unknow.server.servlet.impl;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import unknow.server.servlet.HttpAdapter;
import unknow.server.servlet.HttpError;

/**
 * @author unknow
 */
public class ServletResponseImpl implements HttpServletResponse {
	private static final DateTimeFormatter RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

	private final HttpAdapter co;
	private AbstractServletOutput stream;
	private PrintWriter writer;

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
	 * @param co the connection
	 */
	public ServletResponseImpl(HttpAdapter co) {
		this.co = co;
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
		co.commit();
	}

	public void close() throws IOException {
		if (writer != null)
			writer.close();
		if (stream != null)
			stream.close();
		commit();
	}

	public void checkCommited() {
		if (commited)
			throw new IllegalStateException("already commited");
	}

	public AbstractServletOutput getRawStream() {
		return stream;
	}

	@Override
	public String getCharacterEncoding() {
		return charset == null ? co.ctx().getResponseCharacterEncoding() : charset.name();
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
		if (type == null)
			return null;
		if (!type.contains("charset="))
			type += ";charset=" + getCharacterEncoding();
		return type;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (writer != null)
			throw new IllegalStateException("output already got created with getWriter()");
		if (stream == null)
			stream = co.createOutput();
		return stream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (writer != null)
			return writer;
		if (stream != null)
			throw new IllegalStateException("output already got created with getWriter()");
		stream = co.createOutput();
		return writer = new PrintWriter(new OutputStreamWriter(stream, charset == null ? Charset.forName(co.ctx().getResponseCharacterEncoding()) : charset), false);
	}

	public long getContentLength() {
		return contentLength;
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
		checkCommited();
		bufferSize = size;
		if (stream != null)
			stream.setBufferSize(size);
	}

	@Override
	public int getBufferSize() {
		return stream == null ? bufferSize : stream.getBufferSize();
	}

	@Override
	public void flushBuffer() throws IOException {
		commit();
		if (stream != null)
			stream.flush();
	}

	@Override
	public void resetBuffer() {
		checkCommited();
		if (stream != null)
			stream.resetBuffers();
	}

	@Override
	public void reset() {
		reset(true);
	}

	public void reset(boolean full) {
		resetBuffer();
		status = 200;
		stream = null;
		writer = null;
		if (full) {
			List<String> list = headers.get("connection");
			headers.clear();
			if (list != null)
				headers.put("connection", list);
			charset = null;
			type = null;
			cookies.clear();
			contentLength = -1;
			locale = null;
		}
	}

	@Override
	public final boolean isCommitted() {
		return commited;
	}

	@Override
	public void setLocale(Locale loc) {
		if (charset == null)
			setCharacterEncoding(co.ctx().getEncoding(loc));
		this.locale = loc;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	public Collection<Cookie> getCookies() {
		return cookies;
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
		co.sendError(sc, null, msg);
	}

	@Override
	public void sendError(int sc) throws IOException {
		co.sendError(sc, null, null);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		checkCommited();
		reset();
		commited = true;
		status = HttpError.FOUND.code;
		headers.computeIfAbsent("location", k -> new ArrayList<>(1)).add(location);
		commit();
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
		if ("content-type".equals(name))
			setContentType(value);
		List<String> list = headers.get(name);
		if (list == null)
			headers.put(name, list = new ArrayList<>(1));
		else
			list.clear();
		list.add(value);
	}

	@Override
	public void addHeader(String name, String value) {
		name = name.toLowerCase();
		if ("content-type".equals(name))
			setContentType(value);
		headers.computeIfAbsent(name, k -> new ArrayList<>(1)).add(value);
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
}