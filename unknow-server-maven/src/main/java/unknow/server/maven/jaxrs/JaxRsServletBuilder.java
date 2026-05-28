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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoFailureException;

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
import unknow.maven.codegen.CodeGenUtils;
import unknow.maven.codegen.TypeFactory;
import unknow.model.api.ClassModel;
import unknow.model.api.MethodModel;
import unknow.model.api.ParamModel;
import unknow.model.api.TypeModel;
import unknow.model.jvm.JvmModelLoader;
import unknow.server.http.jaxrs.JaxrsContext;
import unknow.server.http.jaxrs.JaxrsEntityReader;
import unknow.server.http.jaxrs.JaxrsEntityWriter;
import unknow.server.http.jaxrs.JaxrsReq;
import unknow.server.http.jaxrs.PathPattern;
import unknow.server.http.jaxrs.PathPattern.PathRegexp;
import unknow.server.http.jaxrs.PathPattern.PathSimple;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBodyParam;

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

	private final TypeFactory types;
	private final ClassOrInterfaceDeclaration cl;

	private final List<JaxrsMapping> mappings;
	private final Map<String, NameExpr> services = new HashMap<>();

	private final Map<JaxrsParam<?>, String> converterVar = new HashMap<>();

	private final Map<JaxrsMapping, NameExpr> pathParams = new HashMap<>();

	private final ServiceBuilder builder;

	public JaxRsServletBuilder(CompilationUnit cu, Map<String, String> existingClass, String path, List<JaxrsMapping> mappings, BeanParamBuilder beans, MediaTypesBuilder mt) {
		this.cu = cu;
		this.beans = beans;
		this.mt = mt;
		this.mappings = mappings;

		types = new TypeFactory(cu, existingClass);
		cl = cu.addClass(toClass(path), CodeGenUtils.PUBLIC).addSingleMemberAnnotation(WebServlet.class, CodeGenUtils.text(path)).addExtendedType(HttpServlet.class);
		cl.addFieldWithInitializer(long.class, "serialVersionUID", new LongLiteralExpr("1L"), CodeGenUtils.PSF);

		builder = path.endsWith("*") ? new PatternService(path.length() - 1) : new SimpleService();

		for (JaxrsMapping m : mappings) {
			ClassModel c = m.clazz;
			String n = "s$" + services.size();
			if (!services.containsKey(c.name())) {
				services.put(c.name(), new NameExpr(n));
				cl.addFieldWithInitializer(types.get(c.name()), n, new ObjectCreationExpr(null, types.getClass(c), CodeGenUtils.list()), CodeGenUtils.PSF);
			}
		}
	}

	private static String toClass(String path) {
		StringBuilder sb = new StringBuilder("Jaxrs");
		String[] split = path.split("[^a-zA-Z0-9_$*]+");
		for (int i = 0; i < split.length; i++) {
			String s = split[i];
			if (s.isEmpty())
				continue;
			if ("*".equals(s))
				sb.append("Wildcard");
			else
				sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1).toLowerCase());
		}
		return sb.append('_').append(Integer.toHexString(path.hashCode())).toString();
	}

	public CompilationUnit build() throws MojoFailureException {
		buildInializer();
		builder.build();

		for (JaxrsMapping mapping : mappings)
			buildCall(mapping, services);

		return cu;
	}

	private static Expression sendError(String res, int code) {
		return new MethodCallExpr(new NameExpr(res), "sendError", CodeGenUtils.list(new IntegerLiteralExpr(Integer.toString(code))));
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
			getMethod.add(CodeGenUtils.text(m.m.name()));
			getMethod.addAll(classes);

			BlockStmt t = new BlockStmt().addStatement(
					CodeGenUtils.assign(types.getClass(Method.class), "m", new MethodCallExpr(new ClassExpr(types.get(m.m.parent().name())), "getMethod", getMethod)));
			BlockStmt ca = new BlockStmt();

			if (hasParamConverter(m)) {
				t.addStatement(new AssignExpr(new NameExpr("t"), new MethodCallExpr(new NameExpr("m"), "getGenericParameterTypes"), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("a"), new MethodCallExpr(new NameExpr("m"), "getParameterAnnotations"), AssignExpr.Operator.ASSIGN));
				ca.addStatement(new AssignExpr(new NameExpr("t"), CodeGenUtils.array(types.getClass(Type.class), classes), AssignExpr.Operator.ASSIGN)).addStatement(
						new AssignExpr(new NameExpr("a"), CodeGenUtils.array(types.getClass(Annotation.class), parameters.size(), 0), AssignExpr.Operator.ASSIGN));
			}

			if (hasReturn(m)) {
				t.addStatement(new AssignExpr(new NameExpr("r"), new MethodCallExpr(new NameExpr("m"), "getGenericReturnType"), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("ra"), new MethodCallExpr(new NameExpr("m"), "getAnnotations"), AssignExpr.Operator.ASSIGN));
				ca.addStatement(new AssignExpr(new NameExpr("r"), new ClassExpr(types.get(m.m.type().name())), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("ra"), CodeGenUtils.array(types.getClass(Annotation.class), 0), AssignExpr.Operator.ASSIGN));
			}

			b.addStatement(new TryStmt(t, CodeGenUtils.list(new CatchClause(new com.github.javaparser.ast.body.Parameter(types.getClass(Exception.class), "e")
					.addSingleMemberAnnotation(SuppressWarnings.class, CodeGenUtils.text("unused")), ca)), null));

			int i = 0;
			for (JaxrsParam<?> p : m.params)
				processConverter(p, m.v + "$" + i, i++, b);

			TypeModel type = m.m.type();
			if (!type.isVoid()) {
				if (type.isPrimitive())
					type = JvmModelLoader.GLOBAL.get(type.asPrimitive().boxed());
				ClassOrInterfaceType c = types.getClass(type.genericName());
				cl.addField(types.getClass(JaxrsEntityWriter.class, c), m.v + "$r", CodeGenUtils.PSF);
				b.addStatement(new AssignExpr(new NameExpr(m.v + "$r"), new MethodCallExpr(new TypeExpr(types.getClass(JaxrsEntityWriter.class)), "create",
						CodeGenUtils.list(new ClassExpr(types.get(type.name())), new NameExpr("r"), new NameExpr("ra"))), AssignExpr.Operator.ASSIGN));
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

		MethodDeclaration m = cl.addMethod(name, CodeGenUtils.PRIVATE);
		m.addAndGetParameter(types.getClass(JaxrsReq.class), "req").addSingleMemberAnnotation(SuppressWarnings.class, new StringLiteralExpr("unused"));
		m.addParameter(types.getClass(HttpServletResponse.class), "res").createBody()
				.addStatement(new MethodCallExpr(new NameExpr("res"), "setHeader", CodeGenUtils.list(CodeGenUtils.text("Allow"), CodeGenUtils.text(sb.toString()))));
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
			cl.addField(types.getClass(JaxrsEntityReader.class, type), n, CodeGenUtils.PSF);
			b.addStatement(new AssignExpr(new NameExpr(n),
					new ObjectCreationExpr(null, types.getClass(JaxrsEntityReader.class, TypeFactory.EMPTY), CodeGenUtils.list(new ClassExpr(types.get(p.type.name())),
							new ArrayAccessExpr(new NameExpr("t"), new IntegerLiteralExpr("" + i)), new ArrayAccessExpr(new NameExpr("a"), new IntegerLiteralExpr("" + i)))),
					AssignExpr.Operator.ASSIGN));
		} else {
			cl.addField(types.getClass(ParamConverter.class, types.getClass(t)), n, CodeGenUtils.PSF);
			b.addStatement(new AssignExpr(new NameExpr(n),
					new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "converter",
							CodeGenUtils.list(new ClassExpr(types.get(t1.name())),
									new MethodCallExpr(new TypeExpr(types.get(JaxrsContext.class)), "getParamType",
											CodeGenUtils.list(new ArrayAccessExpr(new NameExpr("t"), new IntegerLiteralExpr("" + i)))),
									new ArrayAccessExpr(new NameExpr("a"), new IntegerLiteralExpr("" + i)))),
					AssignExpr.Operator.ASSIGN));
		}
	}

	/**
	 * @param method
	 * @param mapping
	 * @throws MojoFailureException 
	 */
	private void buildMethod(String name, List<JaxrsMapping> list) throws MojoFailureException {

		BlockStmt b = cl.addMethod(name, CodeGenUtils.PRIVATE).addParameter(types.getClass(JaxrsReq.class), "req")
				.addParameter(types.getClass(HttpServletResponse.class), "res").addThrownException(Exception.class).createBody();

		Map<List<JaxrsMapping>, Collection<String>> consume = buildConsumeMap(list);
		Iterator<Entry<List<JaxrsMapping>, Collection<String>>> it = consume.entrySet().iterator();
		List<JaxrsMapping> def = null;
		while (it.hasNext()) {
			Entry<List<JaxrsMapping>, Collection<String>> e = it.next();
			if (e.getValue().contains("*/*")) {
				it.remove();
				def = e.getKey();
				break;
			}
		}
		if (consume.isEmpty()) {
			buildProduces(b, def);
			return;
		}
		b.addStatement(CodeGenUtils.assign(types.getClass(MediaType.class), "contentType", new MethodCallExpr(new NameExpr("req"), "getContentType")));

		Statement stmt = new ThrowStmt(new ObjectCreationExpr(null, types.getClass(NotSupportedException.class), CodeGenUtils.list()));
		if (def != null)
			stmt = buildProduces(new BlockStmt(), def);
		for (Entry<List<JaxrsMapping>, Collection<String>> e : consume.entrySet()) {
			List<JaxrsMapping> key = e.getKey();
			Collection<String> value = e.getValue();
			stmt = new IfStmt(new MethodCallExpr(mt.predicate(value), "isCompatible", CodeGenUtils.list(new NameExpr("contentType"))), buildProduces(new BlockStmt(), key),
					stmt);
		}
		b.addStatement(stmt);
	}

	private static Map<List<JaxrsMapping>, Collection<String>> buildConsumeMap(List<JaxrsMapping> list) {
		Map<String, List<JaxrsMapping>> map = new LinkedHashMap<>();

		for (JaxrsMapping m : list) {
			for (String c : m.consume)
				map.computeIfAbsent(c, k -> new ArrayList<>()).add(m);
		}

		Map<List<JaxrsMapping>, Collection<String>> group = new HashMap<>();
		for (Entry<String, List<JaxrsMapping>> e : map.entrySet()) {
			String c = e.getKey();
			List<JaxrsMapping> l = e.getValue();
			l.sort((a, b) -> a.hashCode() - b.hashCode()); // only need to be stable for this run
			group.computeIfAbsent(l, k -> new ArrayList<>()).add(c);
		}
		return group;
	}

	private Statement buildProduces(BlockStmt b, Collection<JaxrsMapping> mappings) throws MojoFailureException {

		Map<String, JaxrsMapping> produce = new HashMap<>();
		for (JaxrsMapping m : mappings) {
			for (String p : m.produce) {
				JaxrsMapping other = produce.put(p, m);
				if (other != null)
					throw new MojoFailureException("Duplicate mapping on " + m.m + " and " + other.m);
			}
		}

		MethodCallExpr accept = new MethodCallExpr(new NameExpr("req"), "getAccepted",
				CodeGenUtils.list(mt.predicate(produce.keySet()), mt.type(produce.keySet().iterator().next())));

		JaxrsMapping def = produce.remove("*/*");
		Statement stmt = new ThrowStmt(new ObjectCreationExpr(null, types.getClass(NotAcceptableException.class), CodeGenUtils.list()));

		if (def != null)
			stmt = new ExpressionStmt(new MethodCallExpr(def.v + "$call", new NameExpr("req"), new NameExpr("res")));
		if (produce.isEmpty())
			return b.addStatement(accept).addStatement(stmt);

		Map<JaxrsMapping, List<String>> map = new HashMap<>();
		for (Entry<String, JaxrsMapping> e : produce.entrySet())
			map.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());

		if (map.size() == 1) {
			JaxrsMapping m = map.keySet().iterator().next();
			return b.addStatement(new IfStmt(new BinaryExpr(accept, new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
					new ExpressionStmt(new MethodCallExpr(m.v + "$call", new NameExpr("req"), new NameExpr("res"))), stmt));
		}

		b.addStatement(CodeGenUtils.assign(types.getClass(MediaType.class), "accept", accept));

		for (Entry<JaxrsMapping, List<String>> e : map.entrySet()) {
			stmt = new IfStmt(new MethodCallExpr(mt.predicate(produce.keySet()), "isCompatible", CodeGenUtils.list(new NameExpr("accept"))),
					new ExpressionStmt(new MethodCallExpr(e.getKey().v + "$call", new NameExpr("req"), new NameExpr("res"))), stmt);
		}
		b.addStatement(stmt);
		return b;
	}

	private void buildCall(JaxrsMapping mapping, Map<String, NameExpr> services) {
		BlockStmt b = cl.addMethod(mapping.v + "$call", CodeGenUtils.PSF).addParameter(types.getClass(JaxrsReq.class), "r")
				.addParameter(types.getClass(HttpServletResponse.class), "res").addThrownException(types.getClass(Exception.class)).createBody();
		b.addStatement(new MethodCallExpr(new MethodCallExpr(new NameExpr("r"), "getRequest"), "setAttribute",
				CodeGenUtils.list(CodeGenUtils.text("requestPattern"), CodeGenUtils.text(mapping.path))));
		NameExpr n = pathParams.get(mapping);
		if (n != null)
			b.addStatement(new MethodCallExpr(new NameExpr("r"), "initPaths", CodeGenUtils.list(n)));
		for (JaxrsParam<?> p : mapping.params)
			b.addStatement(CodeGenUtils.assign(types.get(p.type), p.v, getParam(p)));
		MethodModel m = mapping.m;
		NodeList<Expression> arg = mapping.params.stream().map(p -> new NameExpr(p.v)).collect(Collectors.toCollection(() -> new NodeList<>()));

		MethodCallExpr call = new MethodCallExpr(services.get(mapping.clazz.name()), m.name(), arg);
		if (m.type().isVoid()) {
			b.addStatement(call).addStatement(sendError("res", 204));
		} else {
			Expression result = CodeGenUtils.assign(types.get(m.type()), "result", call);
			Expression write = new MethodCallExpr(new NameExpr(mapping.v + "$r"), "write", CodeGenUtils.list(new NameExpr("r"), new NameExpr("result"), new NameExpr("res")));
			if (m.type().isAssignableTo(AutoCloseable.class))
				b.addStatement(new TryStmt().setResources(CodeGenUtils.list(result)).setTryBlock(new BlockStmt().addStatement(write)));
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
			return new MethodCallExpr(new NameExpr(converterVar.get(p)), "read", CodeGenUtils.list(new NameExpr("r")));
		if (p instanceof JaxrsBeanParam)
			return new MethodCallExpr(new NameExpr("BeansReader"), beans.get((JaxrsBeanParam<?>) p), CodeGenUtils.list(new NameExpr("r")));
		return JaxrsModel.getParam(p, types, converterVar);
	}

	private interface ServiceBuilder {
		final NameExpr m = new NameExpr("m");
		final Expression[] p = { new NameExpr("r"), new NameExpr("res") };

		void build() throws MojoFailureException;
	}

	private class SimpleService implements ServiceBuilder {

		@Override
		public void build() throws MojoFailureException {
			Map<String, List<JaxrsMapping>> methods = new HashMap<>();
			for (JaxrsMapping m : mappings)
				methods.computeIfAbsent(m.httpMethod, k -> new ArrayList<>()).add(m);

			Statement i = new ExpressionStmt(sendError("res", 405));
			if (!methods.containsKey("OPTIONS"))
				i = new IfStmt(new MethodCallExpr(CodeGenUtils.text("OPTIONS"), "equals", CodeGenUtils.list(m)), new ExpressionStmt(new MethodCallExpr("doOptions", p)), i);
			if (!methods.containsKey("HEAD") && methods.containsKey("GET"))
				i = new IfStmt(new MethodCallExpr(CodeGenUtils.text("HEAD"), "equals", CodeGenUtils.list(m)), new ExpressionStmt(new MethodCallExpr("doGet", p)), i);
			for (String method : methods.keySet())
				i = new IfStmt(new MethodCallExpr(CodeGenUtils.text(method), "equals", CodeGenUtils.list(m)),
						new ExpressionStmt(new MethodCallExpr("do" + method.charAt(0) + method.substring(1).toLowerCase(), p)), i);

			cl.addMethod("service", CodeGenUtils.PUBLIC).addMarkerAnnotation(Override.class).addParameter(types.getClass(HttpServletRequest.class), "req")
					.addParameter(types.getClass(HttpServletResponse.class), "res").addThrownException(IOException.class).addThrownException(ServletException.class).getBody()
					.get().addStatement(CodeGenUtils.create(types.getClass(JaxrsReq.class), "r", CodeGenUtils.list(new NameExpr("req"), new NullLiteralExpr())))
					.addStatement(CodeGenUtils.assign(types.getClass(String.class), "m", new MethodCallExpr(new NameExpr("req"), "getMethod"))).addStatement(i);

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
						CodeGenUtils.PSF);
			}
			pathParams.put(mapping, n);
		}

		@Override
		public void build() throws MojoFailureException {

			List<Path> list = new ArrayList<>(pattern.keySet());
			list.sort((p1, p2) -> p2.length - p1.length);

			Expression req = new ObjectCreationExpr(null, types.getClass(JaxrsReq.class), CodeGenUtils.list(new NameExpr("req"), new NameExpr("l")));
			BlockStmt b = new BlockStmt().addStatement(new VariableDeclarationExpr(types.getClass(List.class, types.get(String.class)), "l"))
					.addStatement(CodeGenUtils.assign(types.get(String.class), "p", new MethodCallExpr(new NameExpr("req"), "getPathInfo")));

			int i = 0;
			for (Path l : list) {
				String n = "p$" + i;

				Expression e;
				if (l.parts == null)
					e = new ObjectCreationExpr(null, types.getClass(PathRegexp.class), CodeGenUtils.list(CodeGenUtils.text(l.pattern)));
				else {
					NodeList<Expression> a = CodeGenUtils.list(new BooleanLiteralExpr(l.last));
					for (String s : l.parts)
						a.add(CodeGenUtils.text(s));
					e = new ObjectCreationExpr(null, types.getClass(PathSimple.class), a);
				}

				cl.addFieldWithInitializer(types.get(PathPattern.class), n, e, CodeGenUtils.PSF);

				b.addStatement(new AssignExpr(new NameExpr("l"), new MethodCallExpr(new NameExpr(n), "process", CodeGenUtils.list(new NameExpr("p"))), Operator.ASSIGN))
						.addStatement(new IfStmt(
								new BinaryExpr(new NameExpr("l"), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS), new BlockStmt()
										.addStatement(new MethodCallExpr(null, "service$" + (i), CodeGenUtils.list(req, new NameExpr("res")))).addStatement(new ReturnStmt()),
								null));
				i++;
			}

			cl.addMethod("service", CodeGenUtils.PUBLIC).addMarkerAnnotation(Override.class).addParameter(types.getClass(HttpServletRequest.class), "req")
					.addParameter(types.getClass(HttpServletResponse.class), "res").addThrownException(IOException.class).addThrownException(ServletException.class)
					.setBody(b.addStatement(sendError("res", 404)));

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
			Statement i = new ExpressionStmt(sendError("res", 405));
			if (!methods.containsKey("OPTIONS"))
				i = new IfStmt(new MethodCallExpr(CodeGenUtils.text("OPTIONS"), "equals", CodeGenUtils.list(m)), new ExpressionStmt(new MethodCallExpr(name + "$options", p)),
						i);
			if (!methods.containsKey("HEAD") && methods.containsKey("GET"))
				i = new IfStmt(new MethodCallExpr(CodeGenUtils.text("HEAD"), "equals", CodeGenUtils.list(m)), new ExpressionStmt(new MethodCallExpr(name + "$get", p)), i);
			for (String method : methods.keySet())
				i = new IfStmt(new MethodCallExpr(CodeGenUtils.text(method), "equals", CodeGenUtils.list(m)),
						new ExpressionStmt(new MethodCallExpr(name + "$" + method.toLowerCase(), p)), i);
			BlockStmt b = new BlockStmt().addStatement(CodeGenUtils.assign(types.getClass(String.class), "m", new MethodCallExpr(new NameExpr("r"), "getMethod")))
					.addStatement(i);

			cl.addMethod(name,
					CodeGenUtils.PRIVATE).addParameter(types.getClass(JaxrsReq.class),
							"r")
					.addParameter(types.getClass(HttpServletResponse.class), "res").createBody()
					.addStatement(new TryStmt(b,
							CodeGenUtils.list(new CatchClause(new com.github.javaparser.ast.body.Parameter(types.getClass(Throwable.class), "e"),
									new BlockStmt().addStatement(new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "sendError",
											CodeGenUtils.list(new NameExpr("r"), new NameExpr("e"), new NameExpr("res")))))),
							null));
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
