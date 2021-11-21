package unknow.server.http.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author unknow
 */
@WebServlet(urlPatterns = { "/test", "/debug/*" }, name = "test", loadOnStartup = 1, initParams = @WebInitParam(name = "content", value = "it works"))
public class Servlet extends HttpServlet implements ServletRequestListener, Filter {
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
		if (!"it works".equals(getInitParameter("content")))
			throw new ServletException("wrong init-param content; " + getInitParameter("content"));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		PrintWriter w = resp.getWriter();
		w.write("Headers:\n");
		Enumeration<String> e = req.getHeaderNames();
		while (e.hasMoreElements()) {
			String k = e.nextElement();
			Enumeration<String> headers = req.getHeaders(k);
			w.write(k);
			w.write(":");
			while (headers.hasMoreElements())
				w.write(" '" + headers.nextElement() + "'");
			w.write('\n');
		}
		w.write("Cookies:\n");
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				w.write(cookies[i].getName());
				w.write(":");
				w.write(cookies[i].getValue());
				w.write('\n');
			}
		}
		w.write("Parameters:\n");
		e = req.getParameterNames();
		while (e.hasMoreElements()) {
			String k = e.nextElement();
			w.write(k);
			w.write(":");
			String[] v = req.getParameterValues(k);
			for (int i = 0; i < v.length; i++)
				w.write(" '" + v[i] + "'");
			w.write('\n');
		}
		w.write("Content:\n");
		try (BufferedReader r = req.getReader()) {
			char[] c = new char[2048];
			int l;
			while ((l = r.read(c)) > 0)
				w.write(c, 0, l);
		}
		w.write("\n--------------\n");
		w.close();
	}

	@Override
	public void requestInitialized(ServletRequestEvent sre) {
//		ServletRequestImpl req = (ServletRequestImpl) sre.getServletRequest();
//		System.out.println(">> '" + req.getMethod() + "' '" + req.getRequestURI() + "' '" + req.getQueryString() + "'");
//		Enumeration<String> e = req.getHeaderNames();
//		while (e.hasMoreElements()) {
//			String k = e.nextElement();
//			Enumeration<String> headers = req.getHeaders(k);
//			System.out.print(k);
//			System.out.print(":");
//			while (headers.hasMoreElements())
//				System.out.print(" '" + headers.nextElement() + "'");
//			System.out.println();
//		}
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
