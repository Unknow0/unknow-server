/**
 * 
 */
package unknow.server.maven.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import unknow.server.http.servlet.FilterChainImpl;
import unknow.server.http.servlet.FilterChainImpl.ServletFilter;
import unknow.server.http.utils.PathTree;
import unknow.server.http.utils.PathTree.EndNode;
import unknow.server.http.utils.ServletManager;
import unknow.server.maven.Builder;
import unknow.server.maven.Descriptor;
import unknow.server.maven.SD;
import unknow.server.maven.TreePathBuilder;
import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class CreateServletManager extends Builder {
	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		BlockStmt b = cl.addMethod("createServletManager", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).setType(types.get(ServletManager.class)).getBody().get();

		TreePathBuilder tree = new TreePathBuilder();
		NodeList<Expression> servlets = new NodeList<>();
		NodeList<Expression> filters = new NodeList<>();

		Map<Object, NameExpr> names = new HashMap<>();
		Set<String> saw = new HashSet<>();
		for (SD s : descriptor.servlets) {
			tree.addServlet(s);
			String n = "s" + s.index;
			names.put(s.e, new NameExpr(n));
			servlets.add(names.get(s.e));
			ClassOrInterfaceType t = types.get(s.e);
			b.addStatement(assign(t, n, new ObjectCreationExpr(null, t, emptyList())));

			for (String p : s.pattern) {
				if (saw.contains(p))
					System.err.println("duplicate servlet pattern '" + p + "'");
				saw.add(p);
			}
		}
		for (SD f : descriptor.filters) {
			tree.addFilter(f);
			if (names.containsKey(f.e)) {
				filters.add(names.get(f.e));
				continue;
			}
			String n = "f" + f.index;
			names.put(f.e, new NameExpr(n));
			filters.add(names.get(f.e));

			ClassOrInterfaceType t = types.get(f.e);
			b.addStatement(assign(t, n, new ObjectCreationExpr(null, t, emptyList())));
		}
		tree.normalize();

		System.out.println(">> Builder tree");
		System.out.println(tree);

		b.addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.get(ServletManager.class), list(
				array(types.get(Servlet.class), servlets),
				array(types.get(Filter.class), filters),
				treePath(b, tree, null, tree.endingFilter, types, names, new HashSet<>())))));
	}

	private Expression treePath(BlockStmt b, TreePathBuilder tree, String path, Map<String, List<SD>> endingFilter, TypeCache types, Map<Object, NameExpr> names, Set<String> created) {
		NodeList<Expression> childs = new NodeList<>();
		NodeList<Expression> ends = new NodeList<>();
		Expression exact = new NullLiteralExpr();
		Expression def = new NullLiteralExpr();

		Map<String, SD> ending = tree.ending;
		if (!endingFilter.isEmpty()) {
			for (Entry<String, List<SD>> e : endingFilter.entrySet()) {
				SD s = ending != null ? ending.getOrDefault(e.getKey(), tree.def) : tree.def;
				String chain = buildChainArray(b, e.getValue(), s, types, names, created);
				ends.add(new ObjectCreationExpr(null, types.get(EndNode.class), list(byteArray(PathTree.encodePart(e.getKey())), new NameExpr(chain))));
			}
		}
		if (ending != null) {
			for (Entry<String, SD> e : ending.entrySet()) {
				if (endingFilter.containsKey(e.getKey()))
					continue;
				String chain = buildChainArray(b, tree.defFilter, e.getValue(), types, names, created);
				ends.add(new ObjectCreationExpr(null, types.get(EndNode.class), list(byteArray(PathTree.encodePart(e.getKey())), new NameExpr(chain))));
			}
		}

		if (tree.exact != null) {
			String chain = buildChainArray(b, tree.exactsFilter, tree.exact, types, names, created);
			exact = new NameExpr(chain);
		} else if (!tree.exactsFilter.isEmpty() && tree.def != null) {
			String chain = buildChainArray(b, tree.exactsFilter, tree.def, types, names, created);
			exact = new NameExpr(chain);
		}

		if (tree.def != null) {
			String chain = buildChainArray(b, tree.defFilter, tree.def, types, names, created);
			def = new NameExpr(chain);
		}

		for (Entry<String, TreePathBuilder> n : tree.nexts.entrySet())
			childs.add(treePath(b, n.getValue(), n.getKey(), endingFilter, types, names, created));

		return new ObjectCreationExpr(null, types.get(PathTree.class), list(
				path == null ? new NullLiteralExpr() : byteArray(PathTree.encodePart(path)),
				childs.isEmpty() ? new NullLiteralExpr() : array(types.get(PathTree.class), childs),
				ends.isEmpty() ? new NullLiteralExpr() : array(types.get(EndNode.class), ends),
				exact, def));

	}

	private static String buildChainArray(BlockStmt b, List<SD> chains, SD s, TypeCache types, Map<Object, NameExpr> names, Set<String> created) {
		StringBuilder sb = new StringBuilder("a");
		NodeList<Expression> list = new NodeList<>();
		for (DispatcherType t : DispatcherType.values()) {
			String n = buildChains(b, actualFilters(chains, t), s, types, names, created);
			sb.append(n);
			list.add(new NameExpr(n));
		}
		String name = sb.toString();
		if (!created.contains(name)) {
			created.add(name);
			b.addStatement(assign(types.array(FilterChain.class), name, array(types.get(FilterChain.class), list)));
		}
		return name;
	}

	private static String buildChains(BlockStmt b, List<SD> chains, SD s, TypeCache types, Map<Object, NameExpr> names, Set<String> created) {
		String n = name(names, chains.size(), chains, s);
		if (!created.contains(n)) {
			created.add(n);
			b.addStatement(assign(types.get(ServletFilter.class), n, new ObjectCreationExpr(null, types.get(ServletFilter.class), list(names.get(s.e)))));
		}

		int size = b.getStatements().size();
		for (int i = 0; i < chains.size(); i++) {
			String name = name(names, i, chains, s);
			if (created.contains(name))
				break;
			created.add(name);
			b.addStatement(size, assign(types.get(FilterChainImpl.class), name, new ObjectCreationExpr(null, types.get(FilterChainImpl.class), list(names.get(chains.get(i).e), new NameExpr(name(names, i + 1, chains, s))))));
		}
		return name(names, 0, chains, s);
	}

	private static String name(Map<Object, NameExpr> names, int i, List<SD> chains, SD s) {
		StringBuilder sb = new StringBuilder("c");
		for (; i < chains.size(); i++)
			sb.append(names.get(chains.get(i).e).getNameAsString());
		sb.append(names.get(s.e).getNameAsString());
		return sb.toString();
	}

	private static List<SD> actualFilters(List<SD> filters, DispatcherType t) {
		filters = new ArrayList<>(filters);
		Iterator<SD> it = filters.iterator();
		while (it.hasNext()) {
			if (!it.next().dispatcher.contains(t))
				it.remove();
		}
		return filters;
	}
}
