/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author unknow
 */
public class DirectoryServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private Path base;

	@Override
	public void init() throws ServletException {
		base = Paths.get(getInitParameter("base"));
	}

	private Path get(HttpServletRequest req) {
		return base.resolve(req.getServletPath().substring(1));
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Path path = get(req);
		if (!Files.exists(path)) {
			resp.sendError(404);
			return;
		}

		resp.setContentLengthLong(Files.size(path));
		resp.setContentType(getServletContext().getMimeType(req.getServletPath()));
		// TODO add cache config

		ServletOutputStream os = resp.getOutputStream();
		try (InputStream is = Files.newInputStream(path)) {
			byte[] b = new byte[4096];
			int l;
			while ((l = is.read(b)) > 0)
				os.write(b, 0, l);
		}
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		try {
			return Files.getLastModifiedTime(get(req)).to(TimeUnit.MILLISECONDS);
		} catch (IOException e) {
			return -1;
		}
	}
}
