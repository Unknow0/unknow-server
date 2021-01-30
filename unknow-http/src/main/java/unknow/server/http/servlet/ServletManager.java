/**
 * 
 */
package unknow.server.http.servlet;

import java.util.Arrays;
import java.util.Comparator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;

import unknow.server.http.PathMatcher;
import unknow.server.http.servlet.FilterChainImpl.ServletFilter;
import unknow.server.nio.util.Buffers;

/**
 * @author unknow
 */
public final class ServletManager {
	private static final Comparator<SEntry> CMP = (a, b) -> a.m.length() - b.m.length();
	private Servlet[] servlets;
	private Filter[] filters;

	private SEntry[] entries;

	public ServletManager(Servlet[] servlets, SEntry[] entries) {
		this.servlets = servlets;
		this.entries = entries;
		Arrays.sort(entries, CMP);
	}

	public Servlet[] getServlets() {
		return servlets;
	}

	public FilterChain find(ServletRequestImpl req) {
		Buffers p = req.rawPath();
		int l = entries.length;
		for (int i = 0; i < l; i++) {
			SEntry e = entries[i];

			PathMatcher m = e.m;
			if (m.match(p)) {
				req.setPathInfoStart(m.length());
				return buildChain(p, e.s);
			}
		}
		return null;
	}

	private FilterChain buildChain(Buffers p, ServletFilter s) {

		return s;
	}

	public static final class SEntry {
		private final PathMatcher m;
		private final ServletFilter s;

		public SEntry(PathMatcher m, Servlet s) {
			this.m = m;
			this.s = new ServletFilter(s);
		}
	}
}
