/**
 * 
 */
package unknow.server.http.utils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;

import unknow.server.http.servlet.ServletRequestImpl;

/**
 * @author unknow
 */
public final class ServletManager {
	private Servlet[] servlets;
	private Filter[] filters;
	private PathTree tree;

	public ServletManager(Servlet[] servlets, Filter[] filters, PathTree tree) {
		this.servlets = servlets;
		this.filters = filters;
		this.tree = tree;
	}

	public Servlet[] getServlets() {
		return servlets;
	}

	public Filter[] getFilters() {
		return filters;
	}

	public FilterChain find(ServletRequestImpl req) {
		FilterChain[] find = tree.find(req);
		return find == null ? null : find[req.getDispatcherType().ordinal()];
	}
}
