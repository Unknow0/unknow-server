/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import unknow.server.http.utils.ArrayMap;
import unknow.server.http.utils.Resource;
import unknow.server.nio.util.Buffers.Chunk;

/**
 * @author unknow
 */
public class DefaultServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private final ArrayMap<Resource> resources;

	public DefaultServlet(ArrayMap<Resource> resources) {
		this.resources = resources;
	}

	/**
	 * @param req
	 * @param resp
	 * @param b
	 * @throws IOException
	 */
	private void process(HttpServletRequest req, HttpServletResponse resp, boolean content) throws IOException {
		Resource r = resources.get(req.getServletPath());
		if (r == null) {
			resp.sendError(404);
			return;
		}

		resp.setContentLengthLong(r.getSize());
		resp.setContentType(getServletContext().getMimeType(req.getServletPath()));
		// TODO add cache config

		if (!content)
			return;
		ServletOutputStream os = resp.getOutputStream();
		try (InputStream is = getServletContext().getResourceAsStream(req.getServletPath())) {
			Chunk c = Chunk.get();
			byte[] b = c.b;
			int l;
			while ((l = is.read(b)) > 0)
				os.write(b, 0, l);
			Chunk.free(c);
		}
	}

	@Override
	public void init() throws ServletException {
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
		Resource resource = resources.get(req.getServletPath());
		return resource == null ? -1 : resource.getLastModified();
	}
}
