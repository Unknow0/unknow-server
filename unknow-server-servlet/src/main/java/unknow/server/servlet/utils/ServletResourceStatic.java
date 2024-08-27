/**
 * 
 */
package unknow.server.servlet.utils;

import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author unknow
 */
public final class ServletResourceStatic extends ServletResource {
	private static final long serialVersionUID = 1L;

	private final byte[] data;

	public ServletResourceStatic(String path, long lastModified, long size) {
		super(path, lastModified, size);
		if (size > Integer.MAX_VALUE)
			throw new IllegalArgumentException("resource too big to load");
		this.data = new byte[(int) size];
	}

	@Override
	public void init() throws ServletException {
		super.init();

		try (InputStream is = ServletResourceStatic.class.getResourceAsStream(path)) {
			if (is == null)
				throw new ServletException("resource " + path + " not found");
			int i = 0;
			int l;
			while ((l = is.read(data, i, data.length - i)) > 0)
				i += l;
		} catch (IOException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void writeContent(HttpServletResponse resp) throws IOException {
		try (ServletOutputStream os = resp.getOutputStream()) {
			os.write(data);
		}
	}

	@Override
	public String toString() {
		return "ResourceStatic:" + path;
	}
}
