/**
 * 
 */
package unknow.server.http.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

final class FilterChainImpl implements FilterChain {
	private final Filter[] filters;
	private int i;

	public FilterChainImpl(Filter[] filters) {
		this.filters = filters;
		i = 0;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
		filters[i++].doFilter(request, response, this);
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
	}
}