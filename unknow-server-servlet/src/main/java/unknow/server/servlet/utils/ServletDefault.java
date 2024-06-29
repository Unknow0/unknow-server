/**
 * 
 */
package unknow.server.servlet.utils;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author unknow
 */
public class ServletDefault extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * singleton instance
	 */
	public static final ServletDefault INSTANCE = new ServletDefault();

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String method = req.getMethod();
		if ("OPTIONS".equals(method))
			res.setHeader("Allow", "GET,HEAD,OPTIONS,TRACE");
		else if ("TRACE".equals(method))
			doTrace(req, res);
		else
			res.sendError(404);
	}

	@Override
	public String toString() {
		return "ServletDefault";
	}
}
