package unknow.server.servlet.http11;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import jakarta.servlet.http.Cookie;
import unknow.server.nio.NIOConnection.Out;
import unknow.server.servlet.HttpError;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.AbstractServletOutput;
import unknow.server.servlet.impl.ServletRequestImpl;

/** http/1.1 worker */
public final class Http11Worker extends HttpWorker {

	private static final byte[] CRLF = { '\r', '\n' };
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

	private static final String UNKNOWN = "Unknown";

	private final Http11Processor p;
	private final int keepAliveIdle;

	/**
	 * new worker
	 * 
	 * @param p the connection
	 * @param req the request
	 */
	public Http11Worker(Http11Processor p, ServletRequestImpl req) {
		super(p.co(), req);
		this.p = p;
		this.keepAliveIdle = co.getkeepAlive() / 1000;

	}

	@SuppressWarnings("resource")
	@Override
	public AbstractServletOutput createOutput() {
		return new Http11OutputStream(co.getOut(), res);
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
		Http11OutputStream rawStream = (Http11OutputStream) res.getRawStream();
		if (rawStream == null)
			out.write(CONTENT_LENGTH0);
		else if (rawStream.isChunked()) {
			if (rawStream.isClosed()) {
				rawStream.setLength(rawStream.size());
				out.write(CONTENT_LENGTH);
				out.write(Integer.toString(rawStream.size()).getBytes(StandardCharsets.US_ASCII));
				out.write(CRLF);
			} else
				out.write(CHUNKED);
		} else {
			out.write(CONTENT_LENGTH);
			out.write(Long.toString(res.getContentLength()).getBytes(StandardCharsets.US_ASCII));
			out.write(CRLF);
		}

		for (Cookie c : res.getCookies())
			writeCookie(out, c);
		out.write(CRLF);
	}

	@SuppressWarnings("resource")
	@Override
	public final boolean doStart() throws IOException, InterruptedException {
		if ("100-continue".equals(req.getHeader("expect"))) {
			Out out = co.getOut();
			out.write(HttpError.CONTINUE.encoded);
			out.write('\r');
			out.write('\n');
			out.flush();
		}

		String header = req.getHeader("connection");
		if (keepAliveIdle >= 0 && (header == null || "keep-alive".equalsIgnoreCase(header))) {
			res.setHeader("connection", "keep-alive");
			if (keepAliveIdle > 0)
				res.setHeader("keep-alive", "timeout=" + keepAliveIdle);
		} else
			res.setHeader("connection", "close");
		return true;
	}

	@Override
	protected void doDone() throws IOException {
		if (!"keep-alive".equalsIgnoreCase(res.getHeader("connection")))
			co.getOut().close();
		else
			p.requestDone();
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
