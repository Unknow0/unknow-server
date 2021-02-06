/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public final class FilterChainImpl implements FilterChain {
	private final Filter filter;
	private final FilterChain next;

	public FilterChainImpl(Filter filter, FilterChain next) {
		this.filter = filter;
		this.next = next;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		filter.doFilter(request, response, next);
	}

	public static class ServletFilter implements FilterChain, Filter {
		private final Servlet servlet;

		public ServletFilter(Servlet servlet) {
			this.servlet = servlet;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			servlet.service(request, response);
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
			servlet.service(request, response);
		}

		@Override
		public String toString() {
			return servlet.getServletConfig().getServletName();
		}
	}
}