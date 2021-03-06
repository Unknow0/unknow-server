/**
 * 
 */
package unknow.server.maven;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import unknow.server.maven.descriptor.SD;

/**
 * @author unknow
 */
public class TreePathBuilder {
	public final Map<String, TreePathBuilder> nexts;
	public final Map<String, SD> ending;
	public SD exact;
	public SD def;

	public final List<SD> exactsFilter;
	public final List<SD> defFilter;
	public final Map<String, List<SD>> endingFilter;

	public TreePathBuilder() {
		this(true);
	}

	private TreePathBuilder(boolean root) {
		nexts = new HashMap<>();
		ending = root ? new HashMap<>() : null;
		exactsFilter = new ArrayList<>();
		defFilter = new ArrayList<>();
		endingFilter = root ? new HashMap<>() : null;
	}

	public void addServlet(SD s) {
		for (String p : s.pattern) {
			if (p.startsWith("*."))
				ending.put(p.substring(1), s);
			else if (p.endsWith("/*"))
				addPath(p.substring(1, p.length() - 2).split("/")).def = s;
			else if (p.equals(""))
				exact = s;
			else if (p.equals("/"))
				def = s;
			else if (p.startsWith("/"))
				addPath(p.substring(1).split("/")).exact = s;
		}
	}

	public void addFilter(SD f) {
		// TODO servletNames
		for (String p : f.pattern) {
			if (p.startsWith("*.")) {
				List<SD> list = endingFilter.get(p.substring(1));
				if (list == null)
					endingFilter.put(p.substring(1), list = new ArrayList<>());
				list.add(f);
			} else if (p.endsWith("/*")) {
				TreePathBuilder n = addPath(p.substring(1, p.length() - 2).split("/"));
				n.exactsFilter.add(f);
				n.addDefFilter(f);
			} else if (p.equals("/")) {
				addDefFilter(f);
				exactsFilter.add(f);
			} else if (p.equals(""))
				exactsFilter.add(f);
			else if (p.startsWith("/"))
				addPath(p.substring(1).split("/")).exactsFilter.add(f);
		}
	}

	private void addDefFilter(SD f) {
		defFilter.add(f);
		for (TreePathBuilder n : nexts.values())
			n.addDefFilter(f);
	}

	private TreePathBuilder addPath(String[] split) {
		TreePathBuilder node = this;
		for (int i = 0; i < split.length; i++)
			node = node.addPath(split[i]);
		return node;
	}

	private TreePathBuilder addPath(String part) {
		TreePathBuilder n = nexts.get(part);
		if (n == null)
			nexts.put(part, n = new TreePathBuilder(false));
		return n;
	}

	public void normalize() {
		normalize(def);
	}

	private void normalize(SD def) {
		if (this.def == null)
			this.def = def;
		for (TreePathBuilder n : nexts.values())
			n.normalize(this.def);
	}

	@Override
	public String toString() {
		return toString(new StringBuilder(), new StringBuilder()).toString();
	}

	public StringBuilder toString(StringBuilder path, StringBuilder sb) {
		sb.append(path).append('\n');
		if (exact != null || !exactsFilter.isEmpty())
			sb.append("  exa: ").append(exactsFilter).append(exact).append('\n');
		if (def != null || !defFilter.isEmpty())
			sb.append("  def: ").append(defFilter).append(def).append('\n');
		if (ending != null)
			sb.append("  end: ").append(endingFilter).append(ending).append('\n');
		int l = path.length();
		for (Entry<String, TreePathBuilder> e : nexts.entrySet()) {
			path.append('/').append(e.getKey());
			e.getValue().toString(path, sb);
			path.setLength(l);
		}
		return sb;
	}

}
