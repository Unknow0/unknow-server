package unknow.server.http.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author unknow
 */
@WebServlet(urlPatterns = { "/test", "/debug/*", "*.dbg" }, name = "test", loadOnStartup = 1, initParams = @WebInitParam(name = "content", value = "it works"))
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
		w.append("uri:     ").append(req.getRequestURI()).write('\n');
		w.append("url:     ").append(req.getRequestURL()).write('\n');
		w.append("servlet: ").append(req.getServletPath()).write('\n');
		w.append("info:    ").append(req.getPathInfo()).write('\n');
		w.append("query:   ").append(req.getQueryString()).write('\n');
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
		// nothing
	}

	@Override
	public void requestDestroyed(ServletRequestEvent sre) {
		// nothing
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		chain.doFilter(request, response);
	}
}
