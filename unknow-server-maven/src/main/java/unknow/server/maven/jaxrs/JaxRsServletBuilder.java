/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ParamConverter;
import unknow.server.http.jaxrs.JaxrsContext;
import unknow.server.http.jaxrs.JaxrsEntityReader;
import unknow.server.http.jaxrs.JaxrsEntityWriter;
import unknow.server.http.jaxrs.JaxrsReq;
import unknow.server.http.jaxrs.PathPattern;
import unknow.server.http.jaxrs.PathPattern.PathRegexp;
import unknow.server.http.jaxrs.PathPattern.PathSimple;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBodyParam;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.jvm.JvmModelLoader;

/**
 * @author unknow
 */
public class JaxRsServletBuilder {

	static final Comparator<String> MIME = (a, b) -> {
		String[] am = a.split("/");
		String[] bm = b.split("/");
		int c = am[0].compareTo(bm[0]);
		if (c != 0) {
			if ("*".equals(am[0]))
				return -1;
			if ("*".equals(bm[0]))
				return 1;
			return c;
		}
		c = am[1].compareTo(bm[1]);
		if (c == 0)
			return 0;
		if ("*".equals(am[1]))
			return -1;
		if ("*".equals(bm[1]))
			return 1;
		return c;
	};

	private final CompilationUnit cu;
	private final BeanParamBuilder beans;
	private final MediaTypesBuilder mt;

	private final TypeCache types;
	private final ClassOrInterfaceDeclaration cl;

	private final List<JaxrsMapping> mappings;
	private final Map<String, NameExpr> services = new HashMap<>();

	private final Map<JaxrsParam<?>, String> converterVar = new HashMap<>();

	private final Map<JaxrsMapping, NameExpr> pathParams = new HashMap<>();

	private final ServiceBuilder b;

	public JaxRsServletBuilder(CompilationUnit cu, Map<String, String> existingClass, String path, List<JaxrsMapping> mappings, BeanParamBuilder beans, MediaTypesBuilder mt) {
		this.cu = cu;
		this.beans = beans;
		this.mt = mt;
		this.mappings = mappings;

		types = new TypeCache(cu, existingClass);
		cl = cu.addClass("Jaxrs" + path.replace('/', '_').replace("*", ""), Utils.PUBLIC).addSingleMemberAnnotation(WebServlet.class, Utils.text(path))
				.addExtendedType(HttpServlet.class);
		cl.addFieldWithInitializer(long.class, "serialVersionUID", new LongLiteralExpr("1L"), Utils.PSF);

		b = path.endsWith("*") ? new PatternService(path.length() - 1) : new SimpleService();

		for (JaxrsMapping m : mappings) {
			ClassModel c = m.clazz;
			String n = "s$" + services.size();
			if (!services.containsKey(c.name())) {
				services.put(c.name(), new NameExpr(n));
				cl.addFieldWithInitializer(types.get(c.name()), n, new ObjectCreationExpr(null, types.getClass(c), Utils.list()), Utils.PSF);
			}
		}
	}

	public CompilationUnit build() {
		buildInializer();
		b.build();

		for (JaxrsMapping mapping : mappings)
			buildCall(mapping, services);

		return cu;
	}

	/**
	 * @param mappings
	 */
	private void buildInializer() {
		BlockStmt b = cl.addStaticInitializer();
		if (hasParamConverter(mappings))
			b.addStatement(new VariableDeclarationExpr(types.array(Type.class), "t"))
					.addStatement(new VariableDeclarationExpr(new ArrayType(types.array(Annotation.class)), "a"));
		if (hasReturn(mappings))
			b.addStatement(new VariableDeclarationExpr(types.getClass(Type.class), "r")).addStatement(new VariableDeclarationExpr(types.array(Annotation.class), "ra"));

		for (JaxrsMapping m : mappings) {
			List<ParamModel<MethodModel>> parameters = m.m.parameters();
			NodeList<Expression> classes = new NodeList<>();
			for (ParamModel<MethodModel> p : parameters)
				classes.add(new ClassExpr(types.get(p.type().name())));
			NodeList<Expression> getMethod = new NodeList<>();
			getMethod.add(Utils.text(m.m.name()));
			getMethod.addAll(classes);

			BlockStmt t = new BlockStmt()
					.addStatement(Utils.assign(types.getClass(Method.class), "m", new MethodCallExpr(new ClassExpr(types.get(m.m.parent().name())), "getMethod", getMethod)));
			BlockStmt ca = new BlockStmt();

			if (hasParamConverter(m)) {
				t.addStatement(new AssignExpr(new NameExpr("t"), new MethodCallExpr(new NameExpr("m"), "getGenericParameterTypes"), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("a"), new MethodCallExpr(new NameExpr("m"), "getParameterAnnotations"), AssignExpr.Operator.ASSIGN));
				ca.addStatement(new AssignExpr(new NameExpr("t"), Utils.array(types.getClass(Type.class), classes), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("a"), Utils.array(types.getClass(Annotation.class), parameters.size(), 0), AssignExpr.Operator.ASSIGN));
			}

			if (hasReturn(m)) {
				t.addStatement(new AssignExpr(new NameExpr("r"), new MethodCallExpr(new NameExpr("m"), "getGenericReturnType"), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("ra"), new MethodCallExpr(new NameExpr("m"), "getAnnotations"), AssignExpr.Operator.ASSIGN));
				ca.addStatement(new AssignExpr(new NameExpr("r"), new ClassExpr(types.get(m.m.type().name())), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("ra"), Utils.array(types.getClass(Annotation.class), 0), AssignExpr.Operator.ASSIGN));
			}

			b.addStatement(new TryStmt(t, Utils.list(new CatchClause(
					new com.github.javaparser.ast.body.Parameter(types.getClass(Exception.class), "e").addSingleMemberAnnotation(SuppressWarnings.class, Utils.text("unused")),
					ca)), null));

			int i = 0;
			for (JaxrsParam<?> p : m.params)
				processConverter(p, m.v + "$" + i, i++, b);

			TypeModel type = m.m.type();
			if (!type.isVoid()) {
				if (type.isPrimitive())
					type = type.asPrimitive().boxed();
				ClassOrInterfaceType c = types.getClass(type.genericName());
				cl.addField(types.getClass(JaxrsEntityWriter.class, c), m.v + "$r", Utils.PSF);
				b.addStatement(new AssignExpr(new NameExpr(m.v + "$r"), new MethodCallExpr(new TypeExpr(types.getClass(JaxrsEntityWriter.class)), "create",
						Utils.list(new ClassExpr(types.get(type.name())), new NameExpr("r"), new NameExpr("ra"))), AssignExpr.Operator.ASSIGN));
			}
			for (JaxrsParam<?> p : m.params) {
				if (p instanceof JaxrsBeanParam)
					beans.add((JaxrsBeanParam<?>) p);
			}
		}
	}

	private static boolean hasParamConverter(Collection<JaxrsMapping> mappings) {
		for (JaxrsMapping m : mappings) {
			if (hasParamConverter(m))
				return true;
		}
		return false;
	}

	private static boolean hasParamConverter(JaxrsMapping m) {
		for (JaxrsParam<?> p : m.params) {
			if (!(p instanceof JaxrsBeanParam))
				return true;
		}
		return false;
	}

	private static boolean hasReturn(Collection<JaxrsMapping> mappings) {
		for (JaxrsMapping m : mappings) {
			if (hasReturn(m))
				return true;
		}
		return false;
	}

	private static boolean hasReturn(JaxrsMapping m) {
		return !m.m.type().isVoid();
	}

	private void buildOptions(String name, Set<String> methods) {
		StringBuilder sb = new StringBuilder("OPTIONS");
		for (String s : methods)
			sb.append(',').append(s);
		if (!methods.contains("HEAD") && methods.contains("GET"))
			sb.append(",HEAD");

		MethodDeclaration m = cl.addMethod(name, Utils.PRIVATE);
		m.addAndGetParameter(types.getClass(JaxrsReq.class), "req").addSingleMemberAnnotation(SuppressWarnings.class, new StringLiteralExpr("unused"));
		m.addParameter(types.getClass(HttpServletResponse.class), "res").createBody()
				.addStatement(new MethodCallExpr(new NameExpr("res"), "setHeader", Utils.list(Utils.text("Allow"), Utils.text(sb.toString()))));
	}

	/**
	 * @param p
	 * @param string
	 * @param b
	 */
	private void processConverter(JaxrsParam<?> p, String n, int i, BlockStmt b) {
		if (p instanceof JaxrsBeanParam)
			return;
		converterVar.put(p, n);

		TypeModel t = JaxrsModel.getParamType(p.type);
		TypeModel t1 = t;
		if (t.isWildCard()) {
			t1 = t.asWildcard().bound();
			if (t1 == null)
				t1 = JvmModelLoader.GLOBAL.get(Object.class.getName());
		}

		if (p instanceof JaxrsBodyParam) {
			ClassOrInterfaceType type = types.getClass(p.type.genericName());
			cl.addField(types.getClass(JaxrsEntityReader.class, type), n, Utils.PSF);
			b.addStatement(new AssignExpr(new NameExpr(n),
					new ObjectCreationExpr(null, types.getClass(JaxrsEntityReader.class, TypeCache.EMPTY), Utils.list(new ClassExpr(types.get(p.type.name())),
							new ArrayAccessExpr(new NameExpr("t"), new IntegerLiteralExpr("" + i)), new ArrayAccessExpr(new NameExpr("a"), new IntegerLiteralExpr("" + i)))),
					AssignExpr.Operator.ASSIGN));
		} else {
			cl.addField(types.getClass(ParamConverter.class, types.getClass(t)), n, Utils.PSF);
			b.addStatement(new AssignExpr(new NameExpr(n),
					new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "converter",
							Utils.list(new ClassExpr(types.get(t1.name())),
									new MethodCallExpr(new TypeExpr(types.get(JaxrsContext.class)), "getParamType",
											Utils.list(new ArrayAccessExpr(new NameExpr("t"), new IntegerLiteralExpr("" + i)))),
									new ArrayAccessExpr(new NameExpr("a"), new IntegerLiteralExpr("" + i)))),
					AssignExpr.Operator.ASSIGN));
		}
	}

	/**
	 * @param method
	 * @param mapping
	 */
	private void buildMethod(String name, List<JaxrsMapping> list) {
		BlockStmt b = new BlockStmt();
		cl.addMethod(name, Utils.PRIVATE).addParameter(types.getClass(JaxrsReq.class), "req").addParameter(types.getClass(HttpServletResponse.class), "res")
				.addThrownException(IOException.class).createBody()
				.addStatement(new TryStmt(b,
						Utils.list(new CatchClause(new com.github.javaparser.ast.body.Parameter(types.getClass(Throwable.class), "e"),
								new BlockStmt().addStatement(new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "sendError",
										Utils.list(new NameExpr("req"), new NameExpr("e"), new NameExpr("res")))))),
						null));

		Map<String, Map<String, JaxrsMapping>> consume = new LinkedHashMap<>();
		for (JaxrsMapping mapping : list) {
			for (int i = 0; i < mapping.consume.length; i++) {
				String c = mapping.consume[i];
				Map<String, JaxrsMapping> map = consume.get(c);
				if (map == null)
					consume.put(c, map = new LinkedHashMap<>());

				for (int j = 0; j < mapping.produce.length; j++) {
					String p = mapping.produce[j];
					JaxrsMapping m = map.get(p);
					if (map.containsKey(p))
						throw new RuntimeException("Duplicate mapping on " + m.m + " and " + mapping.m);
					map.put(p, mapping);
				}
			}
		}

		Map<String, JaxrsMapping> def = consume.remove("*/*");
		if (consume.isEmpty())
			buildProduces(b, def);
		else {
			b.addStatement(Utils.assign(types.getClass(MediaType.class), "contentType", new MethodCallExpr(new NameExpr("req"), "getContentType")));
			List<String> k = new ArrayList<>(consume.keySet());
			k.sort(MIME);

			Statement stmt = def == null ? new ThrowStmt(new ObjectCreationExpr(null, types.getClass(NotSupportedException.class), Utils.list()))
					: buildProduces(new BlockStmt(), def);
			for (String s : k)
				stmt = new IfStmt(new MethodCallExpr(new NameExpr("contentType"), "isCompatible", Utils.list(mt.type(s))), buildProduces(new BlockStmt(), consume.get(s)),
						stmt);
			b.addStatement(stmt);
		}
	}

	private Statement buildProduces(BlockStmt b, Map<String, JaxrsMapping> produce) {

		MethodCallExpr accept = new MethodCallExpr(new NameExpr("req"), "getAccepted",
				Utils.list(mt.predicate(produce.keySet()), mt.type(produce.keySet().iterator().next())));

		JaxrsMapping def = produce.remove("*/*");
		Statement stmt = new ThrowStmt(new ObjectCreationExpr(null, types.getClass(NotAcceptableException.class), Utils.list()));

		if (def != null)
			stmt = new ExpressionStmt(new MethodCallExpr(def.v + "$call", new NameExpr("req"), new NameExpr("res")));
		if (produce.isEmpty())
			return b.addStatement(accept).addStatement(stmt);

		b.addStatement(Utils.assign(types.getClass(MediaType.class), "accept", accept))
				.addStatement(new IfStmt(new BinaryExpr(new NameExpr("accept"), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
						new ThrowStmt(new ObjectCreationExpr(null, types.getClass(NotAcceptableException.class), Utils.list())), null));

		List<String> k = new ArrayList<>(produce.keySet());
		k.sort(MIME);
		for (String s : k) {
			stmt = new IfStmt(new MethodCallExpr(new NameExpr("accept"), "isCompatible", Utils.list(mt.type(s))),
					new ExpressionStmt(new MethodCallExpr(produce.get(s).v + "$call", new NameExpr("req"), new NameExpr("res"))), stmt);
		}
		b.addStatement(stmt);
		return b;
	}

	private void buildCall(JaxrsMapping mapping, Map<String, NameExpr> services) {
		BlockStmt b = cl.addMethod(mapping.v + "$call", Utils.PSF).addParameter(types.getClass(JaxrsReq.class), "r")
				.addParameter(types.getClass(HttpServletResponse.class), "res").addThrownException(types.getClass(Exception.class)).createBody();
		NameExpr n = pathParams.get(mapping);
		if (n != null)
			b.addStatement(new MethodCallExpr(new NameExpr("r"), "initPaths", Utils.list(n)));
		for (JaxrsParam<?> p : mapping.params)
			b.addStatement(Utils.assign(types.get(p.type), p.v, getParam(p)));
		MethodModel m = mapping.m;
		NodeList<Expression> arg = mapping.params.stream().map(p -> new NameExpr(p.v)).collect(Collectors.toCollection(() -> new NodeList<>()));

		MethodCallExpr call = new MethodCallExpr(services.get(mapping.clazz.name()), m.name(), arg);
		if (m.type().isVoid()) {
			b.addStatement(call).addStatement(new MethodCallExpr(new NameExpr("res"), "sendError", Utils.list(new IntegerLiteralExpr("204"))));
		} else {
			Expression result = Utils.assign(types.get(m.type()), "result", call);
			Expression write = new MethodCallExpr(new NameExpr(mapping.v + "$r"), "write", Utils.list(new NameExpr("r"), new NameExpr("result"), new NameExpr("res")));
			if (m.type().isAssignableTo(AutoCloseable.class))
				b.addStatement(new TryStmt().setResources(Utils.list(result)).setTryBlock(new BlockStmt().addStatement(write)));
			else
				b.addStatement(result).addStatement(write);
		}
	}

	/**
	 * @param key
	 * @param value
	 */

	private Expression getParam(JaxrsParam<?> p) {
		if (p instanceof JaxrsBodyParam)
			return new MethodCallExpr(new NameExpr(converterVar.get(p)), "read", Utils.list(new NameExpr("r")));
		if (p instanceof JaxrsBeanParam)
			return new MethodCallExpr(new NameExpr("BeansReader"), beans.get((JaxrsBeanParam<?>) p), Utils.list(new NameExpr("r")));
		return JaxrsModel.getParam(p, types, converterVar);
	}

	private interface ServiceBuilder {
		final NameExpr m = new NameExpr("m");
		final Expression[] p = { new NameExpr("r"), new NameExpr("res") };

		void build();
	}

	private class SimpleService implements ServiceBuilder {

		@Override
		public void build() {
			Map<String, List<JaxrsMapping>> methods = new HashMap<>();
			for (JaxrsMapping m : mappings)
				methods.computeIfAbsent(m.httpMethod, k -> new ArrayList<>()).add(m);

			Statement i = new ExpressionStmt(new MethodCallExpr(new NameExpr("res"), "sendError", Utils.list(new IntegerLiteralExpr("405"))));
			if (!methods.containsKey("OPTIONS"))
				i = new IfStmt(new MethodCallExpr(Utils.text("OPTIONS"), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr("doOptions", p)), i);
			if (!methods.containsKey("HEAD") && methods.containsKey("GET"))
				i = new IfStmt(new MethodCallExpr(Utils.text("HEAD"), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr("doGet", p)), i);
			for (String method : methods.keySet())
				i = new IfStmt(new MethodCallExpr(Utils.text(method), "equals", Utils.list(m)),
						new ExpressionStmt(new MethodCallExpr("do" + method.charAt(0) + method.substring(1).toLowerCase(), p)), i);

			cl.addMethod("service", Utils.PUBLIC).addMarkerAnnotation(Override.class).addParameter(types.getClass(HttpServletRequest.class), "req")
					.addParameter(types.getClass(HttpServletResponse.class), "res").addThrownException(IOException.class).addThrownException(ServletException.class).getBody()
					.get().addStatement(Utils.create(types.getClass(JaxrsReq.class), "r", Utils.list(new NameExpr("req"), new NullLiteralExpr())))
					.addStatement(Utils.assign(types.getClass(String.class), "m", new MethodCallExpr(new NameExpr("req"), "getMethod"))).addStatement(i);

			if (!methods.containsKey("OPTIONS"))
				buildOptions("doOptions", methods.keySet());

			for (Entry<String, List<JaxrsMapping>> e : methods.entrySet()) {
				String m = e.getKey();
				buildMethod("do" + m.charAt(0) + m.substring(1).toLowerCase(), e.getValue());
			}
		}
	}

	private static final Pattern pa = Pattern.compile("\\{\\s*(\\w[\\w\\.\\-]*)\\s*(?::\\s*((?:[^\\{\\}]|\\{[^\\{\\}]*\\})*)\\s*)?\\}");

	private class PatternService implements ServiceBuilder {
		private final Map<Path, Map<String, List<JaxrsMapping>>> pattern = new HashMap<>();
		private final Map<Map<String, Integer>, NameExpr> params = new HashMap<>();

		PatternService(int l) {
			for (JaxrsMapping m : mappings)
				addPath(m, m.path.substring(l));
		}

		void addPath(JaxrsMapping mapping, String path) {
			Map<String, Integer> map = new HashMap<>();
			StringBuilder sb = new StringBuilder("/");
			List<String> parts = new ArrayList<>();
			Matcher m = pa.matcher(path);
			int i = 0;
			int l = 0;
			boolean last = true;
			while (m.find()) {
				l += m.start() - i;
				String s = path.substring(i, m.start());
				sb.append(s.replaceAll("([\\\\.+*\\[\\{])", "\\$1"));
				if (parts != null && !s.isEmpty())
					parts.add(s.substring(1));
				map.put(m.group(1), map.size());
				if (m.group(2) != null) {
					sb.append('(').append(m.group(2)).append(')');
					parts = null;
				} else
					sb.append("([^/]+)");
				i = m.end();
			}
			if (i < path.length()) {
				l += path.length() - i;
				sb.append(path.substring(i));
				if (parts != null) {
					last = false;
					parts.add(path.substring(i + 1));
				}
			}

			pattern.computeIfAbsent(new Path(l, sb.toString(), parts, last), k -> new HashMap<>()).computeIfAbsent(mapping.httpMethod, k -> new ArrayList<>()).add(mapping);
			NameExpr n = params.get(map);
			if (!map.isEmpty() && n == null) {
				params.put(map, n = new NameExpr("path$" + params.size()));
				cl.addFieldWithInitializer(types.getClass(Map.class, types.get(String.class), types.get(Integer.class)), n.getNameAsString(), Utils.mapInteger(map, types),
						Utils.PSF);
			}
			pathParams.put(mapping, n);
		}

		@Override
		public void build() {

			List<Path> list = new ArrayList<>(pattern.keySet());
			list.sort((p1, p2) -> p2.length - p1.length);

			Expression req = new ObjectCreationExpr(null, types.getClass(JaxrsReq.class), Utils.list(new NameExpr("req"), new NameExpr("l")));
			BlockStmt b = new BlockStmt().addStatement(new VariableDeclarationExpr(types.getClass(List.class, types.get(String.class)), "l"))
					.addStatement(Utils.assign(types.get(String.class), "p", new MethodCallExpr(new NameExpr("req"), "getPathInfo")));

			int i = 0;
			for (Path l : list) {
				String n = "p$" + i;

				Expression e;
				if (l.parts == null)
					e = new ObjectCreationExpr(null, types.getClass(PathRegexp.class), Utils.list(Utils.text(l.pattern)));
				else {
					NodeList<Expression> a = Utils.list(new BooleanLiteralExpr(l.last));
					for (String s : l.parts)
						a.add(Utils.text(s));
					e = new ObjectCreationExpr(null, types.getClass(PathSimple.class), a);
				}

				cl.addFieldWithInitializer(types.get(PathPattern.class), n, e, Utils.PSF);

				b.addStatement(new AssignExpr(new NameExpr("l"), new MethodCallExpr(new NameExpr(n), "process", Utils.list(new NameExpr("p"))), Operator.ASSIGN))
						.addStatement(new IfStmt(new BinaryExpr(new NameExpr("l"), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
								new BlockStmt().addStatement(new MethodCallExpr(null, "service$" + (i), Utils.list(req, new NameExpr("res")))).addStatement(new ReturnStmt()),
								null));
				i++;
			}

			cl.addMethod("service", Utils.PUBLIC).addMarkerAnnotation(Override.class).addParameter(types.getClass(HttpServletRequest.class), "req")
					.addParameter(types.getClass(HttpServletResponse.class), "res").addThrownException(IOException.class).addThrownException(ServletException.class)
					.setBody(b.addStatement(new MethodCallExpr(new NameExpr("res"), "sendError", Utils.list(new IntegerLiteralExpr("404")))));

			i = 0;
			for (Path l : list) {
				String name = "service$" + i++;
				Map<String, List<JaxrsMapping>> map = pattern.get(l);
				buildService(name, map);

				if (!map.containsKey("OPTIONS"))
					buildOptions(name + "$options", map.keySet());

				for (Entry<String, List<JaxrsMapping>> e : map.entrySet()) {
					String m = e.getKey();
					buildMethod(name + "$" + m.toLowerCase(), e.getValue());
				}
			}
		}

		private void buildService(String name, Map<String, List<JaxrsMapping>> methods) {
			Statement i = new ExpressionStmt(new MethodCallExpr(new NameExpr("res"), "sendError", Utils.list(new IntegerLiteralExpr("405"))));
			if (!methods.containsKey("OPTIONS"))
				i = new IfStmt(new MethodCallExpr(Utils.text("OPTIONS"), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr(name + "$options", p)), i);
			if (!methods.containsKey("HEAD") && methods.containsKey("GET"))
				i = new IfStmt(new MethodCallExpr(Utils.text("HEAD"), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr(name + "$get", p)), i);
			for (String method : methods.keySet())
				i = new IfStmt(new MethodCallExpr(Utils.text(method), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr(name + "$" + method.toLowerCase(), p)),
						i);

			cl.addMethod(name, Utils.PRIVATE).addParameter(types.getClass(JaxrsReq.class), "r").addParameter(types.getClass(HttpServletResponse.class), "res")
					.addThrownException(IOException.class).setBody(
							new BlockStmt().addStatement(Utils.assign(types.getClass(String.class), "m", new MethodCallExpr(new NameExpr("r"), "getMethod"))).addStatement(i));
		}
	}

	/**
	 * @author unknow
	 */
	private static class Path {
		private final int length;
		private final String pattern;
		private final List<String> parts;
		private final boolean last;

		/**
		 * create new PathPattern
		 * 
		 * @param length
		 * @param pattern
		 * @param last
		 * @param parts
		 * @param count
		 */
		public Path(int length, String pattern, List<String> parts, boolean last) {
			this.length = length;
			this.pattern = pattern;
			this.parts = parts;
			this.last = last;
		}

		@Override
		public int hashCode() {
			return Objects.hash(pattern);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Path))
				return false;
			Path other = (Path) obj;
			return Objects.equals(pattern, other.pattern);
		}

		@Override
		public String toString() {
			return "PathPattern [" + pattern + "]";
		}
	}
}
