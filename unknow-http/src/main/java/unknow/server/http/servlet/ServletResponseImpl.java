/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

/**
 * @author unknow
 */
public class ServletResponseImpl implements ServletResponse {
	private final ServletContextImpl ctx;
	private final OutputStream out;

	private int status;
	private String statusLine;
	private String encoding;
	private String type;
//	private Map<String, List<String>> headers;

	private long contentLength = -1;

	/**
	 * create new ServletResponseImpl
	 * 
	 * @param ctx the context
	 * @param out the raw output
	 */
	public ServletResponseImpl(ServletContextImpl ctx, OutputStream out) {
		this.ctx = ctx;
		this.out = out;
	}

	@Override
	public String getCharacterEncoding() {
		return encoding == null ? ctx.getResponseCharacterEncoding() : encoding;
	}

	@Override
	public void setCharacterEncoding(String charset) {
		encoding = charset;
	}

	@Override
	public void setContentType(String type) {
		int i = type.indexOf("charset=");
		if (i > 0) {
			encoding = type.substring(i + 8);
			i = encoding.indexOf(' ');
			if (i > 0)
				encoding = encoding.substring(0, i);
			i = encoding.indexOf(';');
			if (i > 0)
				encoding = encoding.substring(0, i);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		return null;
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
		throw new IllegalArgumentException("unbeffered");
	}

	@Override
	public int getBufferSize() {
		return 0;
	}

	@Override
	public void flushBuffer() throws IOException {
	}

	@Override
	public void resetBuffer() {
		if (isCommitted())
			throw new IllegalStateException("already commited");
	}

	@Override
	public void reset() {
		resetBuffer();
	}

	@Override
	public boolean isCommitted() {
		return true;
	}

	@Override
	public void setLocale(Locale loc) {
		// TODO Auto-generated method stub

	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

}
