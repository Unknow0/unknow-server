/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import unknow.server.http.HttpRawProcessor;
import unknow.server.http.HttpRawRequest;

/**
 * @author unknow
 */
public class Processor implements HttpRawProcessor {

	private ServletContextImpl context;

	private Servlet servlet1$;

	public Processor() {
//		servlet1$.init(null);
//		context.addFilter(null, null)
//		context.addListener(null);
//		context.addServlet(null, null)
	}

	@Override
	public void process(HttpRawRequest request, OutputStream out) throws IOException {
		// create httpServet
		HttpServletRequest r;
		HttpServletResponse res;
		// TODO if mapping
		// call create filterChain for each path
	}
}
