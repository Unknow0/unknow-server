/**
 * 
 */
package unknow.server.servlet.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author unknow
 */
public abstract class ServletResponseImpl implements HttpServletResponse {
	private static final DateTimeFormatter RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

	protected final ChannelHandlerContext ctx;
	private final ServletContextImpl servletCtx;
	private final HttpServletRequest req;

	private final Collection<Cookie> cookies;

	private AbstractServletOutput<?> stream;
	private PrintWriter writer;

	private int bufferSize = 8192;
	private boolean commited = false;

	private Charset charset;

	/**
	 * create new ServletResponseImpl
	 * 
	 * @param ctx netty context
	 * @param servletCtx the servlet context
	 * @param req the request
	 */
	protected ServletResponseImpl(ChannelHandlerContext ctx, ServletContextImpl servletCtx, HttpServletRequest req) {
		this.ctx = ctx;
		this.servletCtx = servletCtx;
		this.req = req;

		this.cookies = new ArrayList<>();
	}

	protected abstract <T extends ServletResponseImpl> AbstractServletOutput<T> rawOutput();

	protected abstract void doCommit() throws IOException, InterruptedException;

	protected abstract void doReset(boolean full);

	/**
	 * commit the response
	 * 
	 * @throws IOException
	 */
	public final void commit() throws IOException, InterruptedException {
		if (commited)
			return;
		commited = true;
		doCommit();
	}

	public void close() throws IOException, InterruptedException {
		if (writer != null)
			writer.close();
		if (stream != null)
			stream.close();
		rawOutput().close();
		commit();
		ctx.flush();
	}

	public void checkCommited() {
		if (commited)
			throw new IllegalStateException("already commited");
	}

	public void sendError(int sc, Throwable t, String msg) throws IOException {
		reset(false);
		FilterChain f = servletCtx.servlets().getError(sc, t);
		if (f != null) {
			try {
				ServletRequestError r = new ServletRequestError(req, sc, t);
				f.doFilter(r, this);
				return;
			} catch (ServletException e) {
				servletCtx.log("Failed to send error", e);
			}
		}
		setStatus(sc);
		if (msg == null)
			msg = HttpResponseStatus.valueOf(sc).reasonPhrase();
		byte[] bytes = new StringBuilder("<html><body><p>Error ").append(sc).append(' ').append(msg.replace("<", "&lt;")).append("</p></body></html>").toString()
				.getBytes(getCharacterEncoding());
		setContentLength(bytes.length);
		try (ServletOutputStream out = getOutputStream()) {
			out.write(bytes);
		}
	}

	public void reset(boolean full) {
		resetBuffer();
		stream = null;
		writer = null;
		doReset(full);
	}

	@Override
	public String getCharacterEncoding() {
		return charset == null ? servletCtx.getResponseCharacterEncoding() : charset.name();
	}

	@Override
	public void setCharacterEncoding(String charset) {
		this.charset = Charset.forName(charset);
	}

	@Override
	public void setContentType(String type) {
		setHeader("content-type", type);
		int i = type.indexOf(";charset=");
		if (i < 0)
			return;
		int l = type.indexOf(';', i);
		if (l < 0)
			l = type.length();
		charset = Charset.forName(type.substring(i + 9, l));
	}

	@Override
	public String getContentType() {
		String type = getHeader("content-type");
		if (!type.contains("charset=")) {
			type += ";charset=" + getCharacterEncoding();
			setHeader("content-type", type);
		}
		return type;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (writer != null)
			throw new IllegalStateException("output already got created with getWriter()");
		if (stream == null)
			stream = rawOutput();
		return stream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (writer != null)
			return writer;
		if (stream != null)
			throw new IllegalStateException("output already got created with getWriter()");
		stream = rawOutput();
		return writer = new PrintWriter(new ServletWriter(stream, charset == null ? Charset.forName(servletCtx.getResponseCharacterEncoding()) : charset));
	}

	@Override
	public void setContentLength(int len) {
		setIntHeader("content-length", len);
	}

	@Override
	public void setContentLengthLong(long len) {
		setHeader("content-length", Long.toString(len));
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
		try {
			commit();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
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

	@Override
	public final boolean isCommitted() {
		return commited;
	}

	@Override
	public void setLocale(Locale loc) {
		if (charset == null)
			setCharacterEncoding(servletCtx.getEncoding(loc));
		setHeader("Content-Language", loc.toLanguageTag());
	}

	@Override
	public Locale getLocale() {
		String header = getHeader("Content-Language");
		return header == null ? null : Locale.forLanguageTag(header);
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
		sendError(sc, null, msg);
	}

	@Override
	public void sendError(int sc) throws IOException {
		sendError(sc, null, null);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		checkCommited();
		reset();
		commited = true;
		setStatus(302);
		setContentLength(0);
		ctx.close();
		setHeader("location", location);
		try {
			commit();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
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
}