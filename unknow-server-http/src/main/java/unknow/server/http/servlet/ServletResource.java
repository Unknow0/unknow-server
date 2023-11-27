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
import unknow.server.util.io.Buffers.Chunk;

/**
 * @author unknow
 */
public final class ServletResource extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private final String path;
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
	private void process(HttpServletRequest req, HttpServletResponse resp, boolean content) throws IOException {
		Integer code = (Integer) req.getAttribute("javax.servlet.error.status_code");
		if (code != null)
			resp.setStatus(code);
		resp.setContentLengthLong(size);
		resp.setContentType(mimeType);

		if (!content)
			return;
		ServletOutputStream os = resp.getOutputStream();
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
			Chunk c = Chunk.get();
			byte[] b = c.b;
			int l;
			while ((l = is.read(b)) > 0)
				os.write(b, 0, l);
			Chunk.free(c);
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
		return "Resource:" + path;
	}
}
