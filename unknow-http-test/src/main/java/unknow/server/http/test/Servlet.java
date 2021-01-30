package unknow.server.http.test;

import java.io.IOException;
import java.util.Enumeration;
import java.util.EventListener;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.DispatcherType.ERROR;
/**
 * @author unknow
 */
@WebServlet(urlPatterns = { "/test/*" }, name = "test", loadOnStartup = 1, initParams = @WebInitParam(name = "content", value = "it works"))
@WebListener
@WebFilter(dispatcherTypes = {DispatcherType.REQUEST,  ERROR})
public class Servlet extends HttpServlet implements ServletRequestListener, Filter {
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
		System.out.println(getInitParameter("content"));
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		resp.getWriter().append("" + req.getContentLength());
//		resp.getWriter().append(getInitParameter("content"));

		System.out.println(">> --------------");
		System.out.println(req.getContentLength());
		System.out.println(req.getRequestURI());
	}

	@Override
	public void requestInitialized(ServletRequestEvent sre) {
		HttpServletRequest req = (HttpServletRequest) sre.getServletRequest();

		System.out.println(">> " + req.getRemoteAddr() + ":" + req.getRemotePort() + " -> " + req.getLocalName() + ":" + req.getLocalPort() + " (" + req.getLocalAddr() + ")");
		System.out.println(req.getMethod() + " " + req.getRequestURI() + " " + req.getQueryString());
		System.out.println(req.getContentLength());
	}

	@Override
	public void requestDestroyed(ServletRequestEvent sre) {
		ServletRequest req = sre.getServletRequest();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		chain.doFilter(request, response);
	}
}
