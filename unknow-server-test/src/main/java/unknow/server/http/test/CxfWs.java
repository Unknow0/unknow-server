/**
 * 
 */
package unknow.server.http.test;

import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.xml.ws.Endpoint;

/**
 * @author unknow
 */
@WebServlet("/cxf-ws/*")
public class CxfWs extends CXFNonSpringServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void finalizeServletInit(ServletConfig servletConfig) throws ServletException {
		try {
			BusFactory.setDefaultBus(getBus());

			Endpoint.publish("/", new Webservice());
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
}
