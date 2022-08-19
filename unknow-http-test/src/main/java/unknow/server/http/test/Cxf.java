/**
 * 
 */
package unknow.server.http.test;

import javax.xml.ws.Endpoint;

import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;

/**
 * @author unknow
 *//*
@WebServlet("/cxf/*")
public class Cxf extends CXFNonSpringServlet {
	private static final long serialVersionUID = 1L;
	
	@Override
	public void loadBus(ServletConfig servletConfig) {
		try {
			super.loadBus(servletConfig);
			BusFactory.setDefaultBus(getBus());

			Endpoint.publish("/ws", new Webservice());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}*/
