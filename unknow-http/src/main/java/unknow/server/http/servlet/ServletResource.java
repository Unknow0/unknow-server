/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import unknow.server.nio.util.Buffers.Chunk;

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
		try (InputStream is = getServletContext().getResourceAsStream(path)) {
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
	protected long getLastModified(HttpServletRequest req) {
		return lastModified;
	}
}
