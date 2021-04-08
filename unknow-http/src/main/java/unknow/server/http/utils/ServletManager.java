/**
 * 
 */
package unknow.server.http.utils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import unknow.server.http.servlet.ServletRequestImpl;

/**
 * @author unknow
 */
public final class ServletManager {
	private Servlet[] servlets;
	private Filter[] filters;
	private PathTree request;

	private final IntArrayMap<FilterChain> errorCode;
	private final ObjectArrayMap<Class<?>, FilterChain> errorClazz;

	public ServletManager(Servlet[] servlets, Filter[] filters, PathTree request, IntArrayMap<FilterChain> errorCode, ObjectArrayMap<Class<?>, FilterChain> errorClazz) {
		this.servlets = servlets;
		this.filters = filters;
		this.request = request;
		this.errorCode = errorCode;
		this.errorClazz = errorClazz;
	}

	public Servlet[] getServlets() {
		return servlets;
	}

	public Filter[] getFilters() {
		return filters;
	}

	public FilterChain find(ServletRequestImpl req) {
		switch (req.getDispatcherType()) {
			case REQUEST:
				return request.find(req.req);
			default:
				return null;
		}
	}

	public FilterChain getError(int code, Throwable t) {
		if (t != null) {
			FilterChain f = errorClazz.get(t.getClass());
			if (f != null)
				return f;
			if (t instanceof ServletException) {
				t = ((ServletException) t).getRootCause();
				f = errorClazz.get(t.getClass());
				if (f != null)
					return f;
			}
		}
		return errorCode.get(code);
	}
}
