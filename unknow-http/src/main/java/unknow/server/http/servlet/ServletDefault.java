/**
 * 
 */
package unknow.server.http.servlet;

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

	public static final ServletDefault INSTANCE = new ServletDefault();

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.sendError(404);
	}

	@Override
	public String toString() {
		return "ServletDefault";
	}
}
