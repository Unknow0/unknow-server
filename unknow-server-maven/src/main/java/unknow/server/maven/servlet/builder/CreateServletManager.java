/**
 * 
 */
package unknow.server.maven.servlet.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;

import unknow.server.http.servlet.FilterChainImpl;
import unknow.server.http.servlet.FilterChainImpl.ServletFilter;
import unknow.server.http.servlet.ResourceServlet;
import unknow.server.http.utils.IntArrayMap;
import unknow.server.http.utils.ObjectArrayMap;
import unknow.server.http.utils.PathTree;
import unknow.server.http.utils.PathTree.Node;
import unknow.server.http.utils.PathTree.PartNode;
import unknow.server.http.utils.Resource;
import unknow.server.http.utils.ServletManager;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;
import unknow.server.maven.servlet.TreePathBuilder;
import unknow.server.maven.servlet.descriptor.Descriptor;
import unknow.server.maven.servlet.descriptor.SD;

/**
 * @author unknow
 */
public class CreateServletManager extends Builder {
	@Override
	public void add(BuilderContext ctx) {
		Descriptor descriptor = ctx.descriptor();
		TypeCache types = ctx.type();
		BlockStmt b = ctx.self().addMethod("createServletManager", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).setType(types.get(ServletManager.class))
				.addMarkerAnnotation(Override.class)
				.getBody().get();

		NodeList<Expression> servlets = new NodeList<>();
		NodeList<Expression> filters = new NodeList<>();

		Map<Object, NameExpr> names = new HashMap<>();
		Set<String> saw = new HashSet<>();
		for (SD s : descriptor.servlets) {
			String n = "s" + s.index;
			names.put(s.clazz, new NameExpr(n));
			ClassOrInterfaceType t = types.get(s.clazz);
			servlets.add(names.get(s.clazz));
			if (ResourceServlet.class.getName().equals(s.clazz)) {
				Resource r = descriptor.resources.get(s.name);
				b.addStatement(Utils.assign(t, n, new ObjectCreationExpr(null, t, Utils.list(new LongLiteralExpr(r.getLastModified() + "L"), new LongLiteralExpr(r.getSize() + "L")))));
			} else
				b.addStatement(Utils.assign(t, n, new ObjectCreationExpr(null, t, Utils.list())));

			for (String p : s.pattern) {
				if (saw.contains(p))
					System.err.println("duplicate servlet pattern '" + p + "'");
				saw.add(p);
			}
		}
		for (SD f : descriptor.filters) {
			if (names.containsKey(f.clazz)) {
				filters.add(names.get(f.clazz));
				continue;
			}
			String n = "f" + f.index;
			names.put(f.clazz, new NameExpr(n));
			filters.add(names.get(f.clazz));

			ClassOrInterfaceType t = types.get(f.clazz);
			b.addStatement(Utils.assign(t, n, new ObjectCreationExpr(null, t, Utils.list())));
		}

		Set<String> created = new HashSet<>();

		b.addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.get(ServletManager.class),
				Utils.list(Utils.array(types.get(Servlet.class), servlets), Utils.array(types.get(Filter.class), filters),
						buildTree(descriptor, DispatcherType.REQUEST, b, types, names, created), errorCode(b, descriptor, types, names, created),
						errorClass(b, descriptor, types, names, created)))));
	}

	private static Expression buildTree(Descriptor descriptor, DispatcherType type, BlockStmt b, TypeCache types, Map<Object, NameExpr> names, Set<String> created) {
		TreePathBuilder tree = new TreePathBuilder();
		for (SD s : descriptor.servlets)
			tree.addServlet(s);
		for (SD f : descriptor.filters) {
			if (f.dispatcher.contains(type))
				tree.addFilter(f);
		}
		tree.normalize();
		System.err.println("tree:\n" + tree);
		return new ObjectCreationExpr(null, types.get(PathTree.class), Utils.list(nodePart(b, type, tree, null, tree.endingFilter, types, names, created)));
	}

	private static Expression nodePart(BlockStmt b, DispatcherType type, TreePathBuilder tree, String path, Map<String, List<SD>> endingFilter, TypeCache t,
			Map<Object, NameExpr> names, Set<String> created) {
		NodeList<Expression> childs = new NodeList<>();
		NodeList<Expression> ends = new NodeList<>();
		Expression exact = new NullLiteralExpr();
		Expression def = new NullLiteralExpr();

		Map<String, SD> ending = tree.ending;
		if (!endingFilter.isEmpty()) {
			for (Entry<String, List<SD>> e : endingFilter.entrySet()) {
				SD s = ending != null ? ending.getOrDefault(e.getKey(), tree.def) : tree.def;
				String chain = buildChains(b, actualFilters(e.getValue(), type), s, t, names, created);
				ends.add(new ObjectCreationExpr(null, t.get(Node.class), Utils.list(Utils.byteArray(PathTree.encodePart(e.getKey())), new NameExpr(chain))));
			}
		}
		if (ending != null) {
			for (Entry<String, SD> e : ending.entrySet()) {
				if (endingFilter.containsKey(e.getKey()))
					continue;
				String chain = buildChains(b, actualFilters(tree.defFilter, type), e.getValue(), t, names, created);
				ends.add(new ObjectCreationExpr(null, t.get(Node.class), Utils.list(Utils.byteArray(PathTree.encodePart(e.getKey())), new NameExpr(chain))));
			}
		}

		if (tree.exact != null)
			exact = new NameExpr(buildChains(b, actualFilters(tree.exactsFilter, type), tree.exact, t, names, created));
		else if (!tree.exactsFilter.isEmpty() && tree.def != null)
			exact = new NameExpr(buildChains(b, actualFilters(tree.exactsFilter, type), tree.def, t, names, created));

		if (tree.def != null)
			def = new NameExpr(buildChains(b, actualFilters(tree.defFilter, type), tree.def, t, names, created));

		List<String> s = new ArrayList<>(tree.nexts.keySet());
		Collections.sort(s, (s1, s2) -> StringUtils.reverse(s1).compareTo(StringUtils.reverse(s2)));
		for (String p : s)
			childs.add(nodePart(b, type, tree.nexts.get(p), p, endingFilter, t, names, created));

		return new ObjectCreationExpr(null, t.get(PartNode.class),
				Utils.list(path == null ? new NullLiteralExpr() : Utils.byteArray(PathTree.encodePart(path)),
						childs.isEmpty() ? new NullLiteralExpr() : Utils.array(t.get(PartNode.class), childs), new NullLiteralExpr(),
						ends.isEmpty() ? new NullLiteralExpr() : Utils.array(t.get(Node.class), ends), exact, def));
	}

	private static String buildChains(BlockStmt b, Collection<SD> chains, SD s, TypeCache t, Map<Object, NameExpr> names, Set<String> created) {
		String n = name(names, chains.size(), chains, s);
		if (!created.contains(n)) {
			created.add(n);
			b.addStatement(Utils.assign(t.get(FilterChain.class), n, new ObjectCreationExpr(null, t.get(ServletFilter.class), Utils.list(names.get(s.clazz)))));
		}

		int size = b.getStatements().size();
		int i = 0;
		for (SD c : chains) {
			String name = name(names, i++, chains, s);
			if (created.contains(name))
				break;
			created.add(name);
			b.addStatement(size, Utils.assign(t.get(FilterChain.class), name,
					new ObjectCreationExpr(null, t.get(FilterChainImpl.class), Utils.list(names.get(c.clazz), new NameExpr(name(names, i, chains, s))))));
		}
		return name(names, 0, chains, s);
	}

	private static String name(Map<Object, NameExpr> names, int i, Collection<SD> chains, SD s) {
		StringBuilder sb = new StringBuilder("c");
		Iterator<SD> it = chains.iterator();
		while (i-- > 0)
			it.next();
		while (it.hasNext())
			sb.append(names.get(it.next().clazz).getNameAsString());
		sb.append(names.get(s.clazz).getNameAsString());
		return sb.toString();
	}

	private static Collection<SD> actualFilters(Collection<SD> filters, DispatcherType t) {
		filters = new ArrayList<>(filters);
		Iterator<SD> it = filters.iterator();
		while (it.hasNext()) {
			if (!it.next().dispatcher.contains(t))
				it.remove();
		}
		return filters;
	}

	private static ObjectCreationExpr errorCode(BlockStmt b, Descriptor descriptor, TypeCache t, Map<Object, NameExpr> names, Set<String> created) {
		NodeList<Expression> k = new NodeList<>();
		NodeList<Expression> v = new NodeList<>();
		List<Integer> l = new ArrayList<>(descriptor.errorCode.keySet());
		Collections.sort(l);
		for (Integer e : l) {
			String path = descriptor.errorCode.get(e);
			SD s = descriptor.findServlet(path);
			if (s == null)
				continue;
			k.add(new IntegerLiteralExpr(e.toString()));
			v.add(new ObjectCreationExpr(null, t.get(FilterChainImpl.ChangePath.class),
					Utils.list(new StringLiteralExpr(path), new NameExpr(buildChains(b, descriptor.findFilters(path, DispatcherType.ERROR), s, t, names, created)))));
		}
		return new ObjectCreationExpr(null, t.get(IntArrayMap.class, TypeCache.EMPTY),
				Utils.list(Utils.array(PrimitiveType.intType(), k), Utils.array(t.get(FilterChain.class), v)));
	}

	private static ObjectCreationExpr errorClass(BlockStmt b, Descriptor descriptor, TypeCache t, Map<Object, NameExpr> names, Set<String> created) {
		NodeList<Expression> k = new NodeList<>();
		NodeList<Expression> v = new NodeList<>();
		List<String> l = new ArrayList<>(descriptor.errorClass.keySet());
		Collections.sort(l);
		for (String e : l) {
			String path = descriptor.errorClass.get(e);
			SD s = descriptor.findServlet(path);
			if (s == null || e.isEmpty())
				continue;
			k.add(new ClassExpr(t.get(e.toString())));
			v.add(new ObjectCreationExpr(null, t.get(FilterChainImpl.ChangePath.class),
					Utils.list(new StringLiteralExpr(path), new NameExpr(buildChains(b, descriptor.findFilters(path, DispatcherType.ERROR), s, t, names, created)))));
		}
		LambdaExpr cmp = new LambdaExpr(Utils.list(new Parameter(TypeCache.EMPTY, "a"), new Parameter(TypeCache.EMPTY, "b")),
				new MethodCallExpr(new MethodCallExpr(Names.a, "getName"), "compareTo", Utils.list(new MethodCallExpr(Names.b, "getName"))));
		return new ObjectCreationExpr(null, t.get(ObjectArrayMap.class, TypeCache.EMPTY),
				Utils.list(Utils.array(t.get(Class.class), k), Utils.array(t.get(FilterChain.class), v), cmp));
	}
}
