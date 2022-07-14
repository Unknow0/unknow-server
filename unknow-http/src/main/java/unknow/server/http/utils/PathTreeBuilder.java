/**
 * 
 */
package unknow.server.http.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;

import unknow.server.http.servlet.FilterChainImpl;
import unknow.server.http.servlet.FilterChainImpl.ServletFilter;
import unknow.server.http.servlet.FilterConfigImpl;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.servlet.ServletDefault;
import unknow.server.http.utils.PathTree.PartNode;

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
	private final Map<String, Servlet> ending;
	private final Map<String, List<Filter>> endingFilter;

	private final DispatcherType dispatcherType;

	private final StringBuilder sb;

	public PathTreeBuilder(ServletConfigImpl[] servlets, FilterConfigImpl[] filters, DispatcherType dispatcherType) {
		this.servlets = servlets;
		this.filters = filters;
		this.dispatcherType = dispatcherType;

		this.chains = new HashMap<>();

		this.root = new Node();
		this.ending = new HashMap<>();
		this.endingFilter = new HashMap<>();

		this.sb = new StringBuilder();
	}

	public PathTree build() {
		for (int i = 0; i < servlets.length; i++)
			addServlet(servlets[i]);
		for (int i = 0; i < filters.length; i++)
			addFilter(filters[i]);

		root.addDefault(ServletDefault.INSTANCE, Collections.emptyList());
		return new PathTree(buildTree(null, root));
	}

	private PartNode buildTree(String part, Node n) {
		PartNode[] nexts = new PartNode[n.nexts.size()];
		int i = 0;
		for (Entry<String, Node> e : n.nexts.entrySet())
			nexts[i++] = buildTree(e.getKey(), e.getValue());
		Arrays.sort(nexts, CMP);
		PartNode pattern = n.pattern == null ? null : buildTree(null, n.pattern);
		PathTree.Node[] ends = EMPTY;
		if (n == root && (!endingFilter.isEmpty() || !ending.isEmpty())) {
			Set<String> parts = new HashSet<>(endingFilter.keySet());
			parts.addAll(ending.keySet());
			ends = new PathTree.Node[parts.size()];
			int j = 0;
			for (String s : parts)
				ends[j++] = new PathTree.Node(s, getChain(ending.getOrDefault(s, n.def), n.defFilter, endingFilter.getOrDefault(s, Collections.emptyList())));
		} else if (!endingFilter.isEmpty()) {
			ends = new PathTree.Node[endingFilter.size()];
			int j = 0;
			for (Entry<String, List<Filter>> e : endingFilter.entrySet())
				ends[j++] = new PathTree.Node(e.getKey(), getChain(n.def, n.defFilter, e.getValue()));
		}
		Arrays.sort(ends, CMP);
		FilterChain exact = getChain(n.exact, n.exactsFilter, Collections.emptyList());
		FilterChain def = getChain(n.def, n.defFilter, Collections.emptyList());

		return new PartNode(part, nexts, pattern, ends, exact, def);
	}

	private FilterChain getChain(String name, Filter f, FilterChain next) {
		FilterChain c = chains.get(name);
		if (c == null)
			chains.put(sb.toString(), c = new FilterChainImpl(f, next));
		return c;
	}

	private FilterChain getChain(Servlet s, List<Filter> filters, List<Filter> filters2) { // TODO optimize object creation
		sb.setLength(0);
		sb.append(s.getClass().getName());
		FilterChain c = chains.get(sb.toString());
		if (c == null)
			chains.put(sb.toString(), c = new ServletFilter(s));
		for (Filter f : filters) {
			sb.append(',').append(f.getClass().getName());
			c = getChain(sb.toString(), f, c);
		}
		for (Filter f : filters2) {
			sb.append(',').append(f.getClass().getName());
			c = getChain(sb.toString(), f, c);
		}
		return c;
	}

	private void addServlet(ServletConfigImpl s) {
		for (String p : s.getMappings()) {
			if (p.startsWith("*."))
				ending.put(p.substring(1), s.getServlet());
			else if (p.equals("/") || p.equals("/*"))
				root.def = s.getServlet();
			else if (p.endsWith("/*"))
				addPath(p.substring(1, p.length() - 2).split("/")).def = s.getServlet();
			else if (p.equals(""))
				root.exact = s.getServlet();
			else if (p.startsWith("/"))
				addPath(p.substring(1).split("/")).exact = s.getServlet();
		}
	}

	private void addFilter(FilterConfigImpl f) {
		if (!f.getDispatcherTypes().contains(dispatcherType))
			return;
		for (String p : f.getUrlPatternMappings())
			addFilter(f, p);
		for (String s : f.getServletNameMappings()) {
			ServletConfigImpl sc = getServlet(s);
			if (sc == null)
				throw new RuntimeException("no servlet named '" + s + "' for filter '" + f.getName());
			for (String p : sc.getMappings())
				addFilter(f, p);
		}
	}

	private void addFilter(FilterConfigImpl f, String url) {
		Filter filter = f.getFilter();
		if (url.startsWith("*.")) {
			List<Filter> list = endingFilter.get(url.substring(1));
			if (list == null)
				endingFilter.put(url.substring(1), list = new ArrayList<>());
			if (!list.contains(filter))
				list.add(filter);
		} else if (url.equals("/") || url.equals("/*")) {
			if (!root.defFilter.contains(filter))
				root.defFilter.add(filter);
			if (!root.exactsFilter.contains(filter))
				root.exactsFilter.add(filter);
		} else if (url.endsWith("/*")) {
			Node n = addPath(url.substring(1, url.length() - 2).split("/"));
			if (!n.exactsFilter.contains(filter))
				n.exactsFilter.add(filter);
			if (!n.defFilter.contains(filter))
				n.defFilter.add(filter);
		} else if (url.equals("")) {
			if (!root.exactsFilter.contains(filter))
				root.exactsFilter.add(filter);
		} else if (url.startsWith("/")) {
			Node n = addPath(url.substring(1).split("/"));
			if (!n.exactsFilter.contains(filter))
				n.exactsFilter.add(filter);
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

	public static class Node {
		public final Map<String, Node> nexts = new HashMap<>();

		public Servlet exact;
		public final List<Filter> exactsFilter = new ArrayList<>();

		public Servlet def;
		public final List<Filter> defFilter = new ArrayList<>();

		public Node pattern;

		public void addDefault(Servlet def, List<Filter> defFilter) {
			if (this.def == null)
				this.def = def;
			if (this.exact == null)
				this.exact = this.def;
			this.defFilter.addAll(defFilter);
			if (pattern != null)
				pattern.addDefault(this.def, this.defFilter);
			for (Node n : nexts.values())
				n.addDefault(this.def, this.defFilter);
		}
	}
}
