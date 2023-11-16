/**
 * 
 */
package unknow.server.http.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import unknow.server.http.servlet.FilterChainImpl;
import unknow.server.http.servlet.FilterChainImpl.ServletFilter;
import unknow.server.http.servlet.FilterConfigImpl;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletDefault;
import unknow.server.http.utils.PathTree.PartNode;
import unknow.server.util.data.ArrayMap;
import unknow.server.util.data.ArraySet;

/**
 * @author unknow
 */
public class PathTreeBuilder {
	private static final PathTree.Node[] EMPTY = new PathTree.Node[0];
	private static final Comparator<PathTree.Node> CMP = (a, b) -> PathTree.compare(a.part, b.part);

	private final ServletConfigImpl[] servlets;
	private final FilterConfigImpl[] filters;

	private final Map<String, FilterChain> chains;

	private final Node root;
	private final Map<String, ServletConfigImpl> ending;
	private final Map<String, Set<FilterConfigImpl>> endingFilter;

	private final DispatcherType dispatcherType;

	private final StringBuilder sb;

	private final ServletConfigImpl defaultServlet;

	public PathTreeBuilder(ServletContextImpl ctx, ServletConfigImpl[] servlets, FilterConfigImpl[] filters, DispatcherType dispatcherType) {
		this.servlets = servlets;
		this.filters = filters;
		this.dispatcherType = dispatcherType;

		this.chains = new HashMap<>();

		this.root = new Node();
		this.ending = new HashMap<>();
		this.endingFilter = new HashMap<>();

		this.sb = new StringBuilder();

		this.defaultServlet = new ServletConfigImpl("", ServletDefault.INSTANCE, ctx, new ArrayMap<>(new String[0], new String[0]), new ArraySet<>(new String[0]));
	}

	public PathTree build() throws ServletException {
		for (int i = 0; i < servlets.length; i++)
			addServlet(servlets[i]);
		for (int i = 0; i < filters.length; i++)
			addFilter(filters[i]);
		root.addDefault(defaultServlet, Collections.emptySet());
		return new PathTree(buildTree(null, root));
	}

	private PartNode buildTree(String part, Node n) {
		PartNode[] nexts = new PartNode[n.nexts.size()];
		int i = 0;
		for (Entry<String, Node> e : n.nexts.entrySet())
			nexts[i++] = buildTree(e.getKey(), e.getValue());
		Arrays.sort(nexts, CMP);
		PathTree.Node[] ends = EMPTY;
		if (n == root && (!endingFilter.isEmpty() || !ending.isEmpty())) {
			Set<String> parts = new HashSet<>(endingFilter.keySet());
			parts.addAll(ending.keySet());
			ends = new PathTree.Node[parts.size()];
			int j = 0;
			for (String s : parts)
				ends[j++] = new PathTree.Node(s, getChain(ending.getOrDefault(s, n.def), n.defFilter, endingFilter.getOrDefault(s, Collections.emptySet())));
		} else if (!endingFilter.isEmpty()) {
			ends = new PathTree.Node[endingFilter.size()];
			int j = 0;
			for (Entry<String, Set<FilterConfigImpl>> e : endingFilter.entrySet())
				ends[j++] = new PathTree.Node(e.getKey(), getChain(n.def, n.defFilter, e.getValue()));
		}
		Arrays.sort(ends, CMP);
		FilterChain exact = getChain(n.exact, n.exactsFilter, Collections.emptySet());
		FilterChain def = getChain(n.def, n.defFilter, Collections.emptySet());

		return new PartNode(part, nexts, ends, exact, def);
	}

	private FilterChain getChain(String name, Filter f, FilterChain next) {
		FilterChain c = chains.get(name);
		if (c == null)
			chains.put(sb.toString(), c = new FilterChainImpl(f, next));
		return c;
	}

	private FilterChain getChain(ServletConfigImpl s, Set<FilterConfigImpl> filters, Set<FilterConfigImpl> filters2) {
		sb.setLength(0);
		sb.append(s.getName());
		FilterChain c = chains.get(sb.toString());
		if (c == null)
			chains.put(sb.toString(), c = new ServletFilter(s.getServlet()));
		for (FilterConfigImpl f : filters) {
			sb.append(',').append(f.getFilterName());
			c = getChain(sb.toString(), f.getFilter(), c);
		}
		for (FilterConfigImpl f : filters2) {
			sb.append(',').append(f.getFilterName());
			c = getChain(sb.toString(), f.getFilter(), c);
		}
		return c;
	}

	private void addServlet(ServletConfigImpl s) {
		for (String p : s.getMappings()) {
			if (p.startsWith("*."))
				ending.put(p.substring(1), s);
			else if (p.equals("/") || p.equals("/*"))
				root.def = s;
			else if (p.endsWith("/*"))
				addPath(p.substring(1, p.length() - 2).split("/")).def = s;
			else if (p.equals(""))
				root.exact = s;
			else if (p.startsWith("/"))
				addPath(p.substring(1).split("/")).exact = s;
		}
	}

	private void addFilter(FilterConfigImpl f) throws ServletException {
		if (!f.getDispatcherTypes().contains(dispatcherType))
			return;
		for (String p : f.getUrlPatternMappings())
			addFilter(f, p);
		for (String s : f.getServletNameMappings()) {
			ServletConfigImpl sc = getServlet(s);
			if (sc == null)
				throw new ServletException("no servlet named '" + s + "' for filter '" + f.getName() + "'");
			for (String p : sc.getMappings())
				addFilter(f, p);
		}
	}

	private void addFilter(FilterConfigImpl f, String url) {
		if (url.startsWith("*.")) {
			endingFilter.computeIfAbsent(url.substring(1), k -> new HashSet<>()).add(f);
		} else if (url.equals("/") || url.equals("/*")) {
			root.defFilter.add(f);
			root.exactsFilter.add(f);
		} else if (url.endsWith("/*")) {
			Node n = addPath(url.substring(1, url.length() - 2).split("/"));
			n.exactsFilter.add(f);
			n.defFilter.add(f);
		} else if (url.equals("")) {
			root.exactsFilter.add(f);
		} else if (url.startsWith("/")) {
			Node n = addPath(url.substring(1).split("/"));
			n.exactsFilter.add(f);
		}
	}

	private Node addPath(String[] split) {
		Node node = root;
		for (int i = 0; i < split.length; i++) {
			String part = split[i];
			Node n = node.nexts.get(part);
			if (n == null)
				node.nexts.put(part, n = new Node());
			node = n;
		}
		return node;
	}

	private ServletConfigImpl getServlet(String name) {
		for (ServletConfigImpl s : servlets) {
			if (s.getName().equals(name))
				return s;
		}
		return null;
	}

	static class Node {
		final Map<String, Node> nexts = new HashMap<>();

		ServletConfigImpl exact;
		final Set<FilterConfigImpl> exactsFilter = new HashSet<>();

		ServletConfigImpl def;
		final Set<FilterConfigImpl> defFilter = new HashSet<>();

		void addDefault(ServletConfigImpl def, Set<FilterConfigImpl> defFilter) {
			if (this.def == null)
				this.def = def;
			if (this.exact == null)
				this.exact = this.def;
			this.defFilter.addAll(defFilter);
			this.exactsFilter.addAll(defFilter);
			for (Node n : nexts.values())
				n.addDefault(this.def, this.defFilter);
		}
	}
}
