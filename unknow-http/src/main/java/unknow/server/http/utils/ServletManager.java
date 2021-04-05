/**
 * 
 */
package unknow.server.http.utils;

import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import unknow.server.http.HttpHandler;
import unknow.server.http.servlet.ServletRequestImpl;

/**
 * @author unknow
 */
public final class ServletManager {
	private Servlet[] servlets;
	private Filter[] filters;
	private PathTree tree;

	private final IntArrayMap<List<byte[]>> errorCode;
	private final ObjectArrayMap<Class<?>, List<byte[]>> errorClazz;

	public ServletManager(Servlet[] servlets, Filter[] filters, PathTree tree, IntArrayMap<List<byte[]>> errorCode, ObjectArrayMap<Class<?>, List<byte[]>> errorClazz) {
		this.servlets = servlets;
		this.filters = filters;
		this.tree = tree;
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
		FilterChain[] find = tree.find(req.req);
		return find == null ? null : find[req.getDispatcherType().ordinal()];
	}

	public List<byte[]> getError(Throwable t) {
		Class<?> cl = t.getClass();
		List<byte[]> list = errorClazz.get(cl);
		while (list == null && cl != Throwable.class)
			list = errorClazz.get((cl = cl.getSuperclass()));
		return list;
	}

	public List<byte[]> getError(int code, Throwable t) {
		if (t != null) {
			List<byte[]> list = errorClazz.get(t.getClass());
			if (list != null)
				return list;
			if (t instanceof ServletException) {
				t = ((ServletException) t).getRootCause();
				list = errorClazz.get(t.getClass());
				if (list != null)
					return list;
			}
		}
		return errorCode.get(code);
	}
}
