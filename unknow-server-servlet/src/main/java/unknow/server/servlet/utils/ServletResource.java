/**
 * 
 */
package unknow.server.servlet.utils;

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
public class ServletResource extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected final String path;
	private final long lastModified;
	private final long size;
	private String mimeType;

	public ServletResource(String path, long lastModified, long size) {
		this.path = path;
		this.lastModified = lastModified;
		this.size = size;
	}

	@Override
	public void init() throws ServletException {
		this.mimeType = getServletContext().getMimeType(path);
	}

	/**
	 * process a request
	 * 
	 * @param req     the request
	 * @param resp    the response
	 * @param content if true file content will be sent
	 * @throws IOException
	 */
	private final void process(HttpServletRequest req, HttpServletResponse resp, boolean content) throws IOException {
		Integer code = (Integer) req.getAttribute("javax.servlet.error.status_code");
		if (code != null)
			resp.setStatus(code);
		resp.setContentLengthLong(size);
		resp.setContentType(mimeType);

		if (!content)
			return;
	}

	protected void writeContent(HttpServletResponse resp) throws IOException {
		try (InputStream is = getServletContext().getResourceAsStream(path); ServletOutputStream os = resp.getOutputStream()) {
			byte[] b = new byte[4096];
			int l;
			while ((l = is.read(b)) > 0)
				os.write(b, 0, l);
		}
	}

	@Override
	protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		process(req, resp, true);
	}

	@Override
	protected final void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		process(req, resp, false);
	}

	@Override
	protected final void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setHeader("Allow", "GET,HEAD,OPTIONS,TRACE");
	}

	@Override
	protected final long getLastModified(HttpServletRequest req) {
		return lastModified;
	}

	@Override
	public String toString() {
		return "Resource:" + path;
	}
}
