/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * implementation of the FilterChain
 * 
 * @author unknow
 */
public final class FilterChainImpl implements FilterChain {
	private final Filter filter;
	private final FilterChain next;

	/**
	 * create new FilterChainImpl
	 * 
	 * @param filter the first filter to run
	 * @param next   the next chain to run
	 */
	public FilterChainImpl(Filter filter, FilterChain next) {
		this.filter = filter;
		this.next = next;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		filter.doFilter(request, response, next);
	}

	@Override
	public String toString() {
		return filter.toString() + "," + next.toString();
	}

	/**
	 * A Filter chain that only run a Servlet
	 * 
	 * @author unknow
	 */
	public static class ServletFilter implements FilterChain {
		private final Servlet servlet;

		/**
		 * create new ServletFilter
		 * 
		 * @param servlet the servlet to run
		 */
		public ServletFilter(Servlet servlet) {
			this.servlet = servlet;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
			servlet.service(request, response);
		}

		@Override
		public String toString() {
			ServletConfig c = servlet.getServletConfig();
			return c == null ? servlet.toString() : c.getServletName();
		}
	}

	public static class ChangePath implements FilterChain {
		private final String path;
		private final FilterChain next;

		public ChangePath(String path, FilterChain next) {
			this.path = path;
			this.next = next;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
			((ServletRequestImpl) request).setServletPath(path);
			next.doFilter(request, response);
		}

	}
}