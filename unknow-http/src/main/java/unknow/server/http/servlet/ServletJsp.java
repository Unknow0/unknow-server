/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author unknow
 */
public abstract class ServletJsp extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
		jspInit();
	}

	@Override
	public void destroy() {
		jspDestroy();
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		_jspService(req, resp);
	}

	/**
	 * @throws ServletException
	 */
	protected void jspInit() throws ServletException {
	}

	protected void jspDestroy() {
	}

	/**
	 * @param req
	 * @param resp
	 */
	protected abstract void _jspService(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
}
