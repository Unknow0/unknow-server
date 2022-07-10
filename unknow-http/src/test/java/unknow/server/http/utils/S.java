/**
 * 
 */
package unknow.server.http.utils;

import javax.servlet.http.HttpServlet;

/**
 * @author unknow
 */
public class S extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final String name;

	public S(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
