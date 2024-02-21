/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author unknow
 */
public final class ServletResourceStatic extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private final String path;
	private final byte[] data;
	private final long lastModified;
	private final long size;
	private String mimeType;

	public ServletResourceStatic(String path, long lastModified, long size) {
		if (size > Integer.MAX_VALUE)
			throw new IllegalArgumentException("resource too big to load");
		this.path = path;
		this.data = new byte[(int) size];
		this.lastModified = lastModified;
		this.size = size;
	}

	@Override
	public void init() throws ServletException {
		this.mimeType = getServletContext().getMimeType(path);

		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
			int i = 0;
			int l;
			while ((l = is.read(data, i, data.length - i)) > 0)
				i += l;
		} catch (IOException e) {
			throw new ServletException(e);
		}
	}

	/**
	 * process a request
	 * 
	 * @param req     the request
	 * @param resp    the response
	 * @param content if true file content will be sent
	 * @throws IOException
	 */
	private void process(HttpServletRequest req, HttpServletResponse resp, boolean content) throws IOException {
		Integer code = (Integer) req.getAttribute("javax.servlet.error.status_code");
		if (code != null)
			resp.setStatus(code);
		resp.setContentLengthLong(size);
		if (mimeType != null)
			resp.setContentType(mimeType);

		if (!content)
			return;

		try (ServletOutputStream os = resp.getOutputStream()) {
			os.write(data);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		process(req, resp, true);
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		process(req, resp, false);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setHeader("Allow", "GET,HEAD,OPTIONS,TRACE");
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		return lastModified;
	}

	@Override
	public String toString() {
		return "ResourceStatic:" + path;
	}
}
