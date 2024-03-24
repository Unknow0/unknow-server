/**
 * 
 */
package unknow.server.servlet.impl;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
	protected void jspInit() throws ServletException { // OK
	}

	protected void jspDestroy() { // OK
	}

	/**
	 * @param req
	 * @param resp
	 */
	protected abstract void _jspService(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
}
