/**
 * 
 */
package unknow.server.http.test;

import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;
import javax.xml.ws.Endpoint;

import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

/**
 * @author unknow
 */
@WebServlet("/cxf/*")
public class Cxf extends CXFNonSpringServlet {
	private static final long serialVersionUID = 1L;

	static {
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger("");
		logger.setLevel(Level.FINEST);
		logger.getHandlers()[0].setLevel(Level.FINEST);
	}

	@Override
	public void loadBus(ServletConfig servletConfig) {
		super.loadBus(servletConfig);

		// You could add the endpoint publish codes here
		BusFactory.setDefaultBus(getBus());
		Endpoint e = Endpoint.create(new Webservice());
		e.publish("/ws");
	}
}
