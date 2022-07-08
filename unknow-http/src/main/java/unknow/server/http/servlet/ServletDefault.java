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
public class ServletDefault extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public static final ServletDefault INSTANCE = new ServletDefault();

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.sendError(404);
	}
}
