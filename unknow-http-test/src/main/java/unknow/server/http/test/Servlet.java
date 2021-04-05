package unknow.server.http.test;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import unknow.server.http.servlet.ServletRequestImpl;

/**
 * @author unknow
 */
@WebServlet(urlPatterns = { "/test/*", "/bla", "/bla/yes/*", "*.test" }, name = "test", loadOnStartup = 1, initParams = @WebInitParam(name = "content", value = "it works"))
@WebListener
public class Servlet extends HttpServlet implements ServletRequestListener, Filter {
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
//		System.out.println(getInitParameter("content"));
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		System.out.println("doGet " + req);

//		PrintWriter append = resp.getWriter();
//		append.append("" + req.getContentLength()).append(getInitParameter("content"));
		resp.setContentLength(30);
		resp.setHeader("test", "bl\"truc");
		resp.getWriter().write("<html><body><h1>It work's</h1></body></html>");
	}

	@Override
	public void requestInitialized(ServletRequestEvent sre) {
		ServletRequestImpl req = (ServletRequestImpl) sre.getServletRequest();
		System.out.println(">> '" + req.getMethod() + "' '" + req.getRequestURI() + "' '" + req.getQueryString() + "'");
		Enumeration<String> e = req.getHeaderNames();
		while (e.hasMoreElements()) {
			String k = e.nextElement();
			Enumeration<String> headers = req.getHeaders(k);
			System.out.print(k);
			System.out.print(":");
			while (headers.hasMoreElements())
				System.out.print(" '" + headers.nextElement() + "'");
			System.out.println();
		}
	}

	@Override
	public void requestDestroyed(ServletRequestEvent sre) {
//		ServletRequest req = sre.getServletRequest();
//		System.out.println("destroy " + req);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//		System.out.println("filtering " + request);
		chain.doFilter(request, response);
	}
}
