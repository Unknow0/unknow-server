/**
 * 
 */
package unknow.server.http.utils;

import java.util.Comparator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import unknow.server.http.servlet.FilterChainImpl;
import unknow.server.http.servlet.FilterChainImpl.ChangePath;
import unknow.server.http.servlet.FilterChainImpl.ServletFilter;
import unknow.server.http.servlet.FilterConfigImpl;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.util.data.IntArrayMap;
import unknow.server.util.data.ObjectArrayMap;

/**
 * @author unknow
 */
public final class ServletManager {
	private static final Comparator<Class<?>> CMP = (a, b) -> a.getName().compareTo(b.getName());
	private static final Logger logger = LoggerFactory.getLogger(ServletManager.class);

	private ServletConfigImpl[] servlets;
	private FilterConfigImpl[] filters;
	private PathTree request;

	private final IntArrayMap<String> errorCodeMapping;
	private final ObjectArrayMap<Class<?>, String> errorClazzMapping;

	private IntArrayMap<FilterChain> errorCode;
	private ObjectArrayMap<Class<?>, FilterChain> errorClazz;

	public ServletManager(IntArrayMap<String> errorCode, ObjectArrayMap<Class<?>, String> errorClazz) {
		this.errorCodeMapping = errorCode;
		this.errorClazzMapping = errorClazz;
	}

	public ServletConfigImpl findServlet(String path) {
		int l = 0;
		ServletConfigImpl s = null;
		for (int i = 0; i < servlets.length; i++) {
			ServletConfigImpl sc = servlets[i];
			for (String p : sc.getMappings()) {
				if ((p.equals("/") || p.equals("/*")) && l == 0)
					s = sc;
				else if (p.length() > l && (p.startsWith("*.") && path.endsWith(p.substring(1)) || p.endsWith("/*") && path.startsWith(p, p.length() - 2))) {
					l = p.length();
					s = sc;
				} else if (p.equals("") && path.equals("/") || p.equals(path))
					return sc;
			}
		}
		return s;
	}

	public Servlet getServlet(int i) {
		return servlets[i].getServlet();
	}

	public Filter getFilter(int i) {
		return filters[i].getFilter();
	}

	public ServletConfigImpl[] getServlets() {
		return servlets;
	}

	public FilterConfigImpl[] getFilters() {
		return filters;
	}

	public FilterChain find(ServletRequestImpl req) throws InterruptedException {
		if (req.getDispatcherType() == DispatcherType.REQUEST)
			return request.find(req);
		System.out.println("WTF");
		return null;
	}

	public void initialize(ServletContextImpl ctx, ServletConfigImpl[] servlets, FilterConfigImpl[] filters) throws ServletException {
		this.servlets = servlets;
		this.filters = filters;
		this.request = new PathTreeBuilder(ctx, servlets, filters, DispatcherType.REQUEST).build();
		logger.info("Servlet mapping:\n{}", this.request);

		int l = errorCodeMapping.size();
		int[] error = new int[l];
		FilterChain[] chain = new FilterChain[l];
		int i = 0;
		for (Integer e : errorCodeMapping.keySet()) {
			String path = errorCodeMapping.get(e);
			ServletConfigImpl s = findServlet(path);
			if (s == null)
				continue;
			error[i] = e;
			chain[i] = buildErrorChain(s, path);
		}
		errorCode = new IntArrayMap<>(error, chain);

		l = errorClazzMapping.size();
		Class<?>[] clazz = new Class[l];
		chain = new FilterChain[l];
		i = 0;
		for (Map.Entry<Class<?>, String> e : errorClazzMapping.entrySet()) {
			ServletConfigImpl s = findServlet(e.getValue());
			if (s == null)
				continue;
			clazz[i] = e.getKey();
			chain[i] = buildErrorChain(s, e.getValue());
		}
		errorClazz = new ObjectArrayMap<>(clazz, chain, CMP);

		for (ServletConfigImpl s : servlets)
			s.getServlet().init(s);
		for (FilterConfigImpl f : this.filters)
			f.getFilter().init(f);
	}

	private static FilterChain buildChain(FilterConfigImpl f, FilterChain chain, String path) {

		for (String p : f.getUrlPatternMappings()) {
			if ((p.equals("/") || p.equals("/*") || p.startsWith("*.") && path.endsWith(p.substring(1)) || p.endsWith("/*") && path.startsWith(p, p.length() - 2))
					|| p.equals("") && path.equals("/") || p.equals(path)) {
				return new FilterChainImpl(f.getFilter(), chain);
			}
		}
		return chain;
	}

	private FilterChain buildErrorChain(ServletConfigImpl s, String path) {
		FilterChain chain = new ServletFilter(s.getServlet());
		String name = s.getServletName();
		for (int i = 0; i < filters.length; i++) {
			FilterConfigImpl f = filters[i];
			if (!f.getDispatcherTypes().contains(DispatcherType.ERROR))
				continue;

			if (f.getServletNameMappings().contains(name)) {
				chain = new FilterChainImpl(f.getFilter(), chain);
				continue;
			}
			chain = buildChain(f, chain, path);
		}
		return new ChangePath(path, chain);
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
