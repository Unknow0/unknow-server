/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
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
import com.github.javaparser.ast.type.TypeParameter;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.RuntimeDelegate;
import unknow.server.http.jaxrs.JaxrsContext;
import unknow.server.http.jaxrs.JaxrsEntityReader;
import unknow.server.http.jaxrs.JaxrsEntityWriter;
import unknow.server.http.jaxrs.JaxrsPath;
import unknow.server.http.jaxrs.JaxrsReq;
import unknow.server.http.jaxrs.JaxrsRuntime;
import unknow.server.http.jaxrs.impl.DefaultConvert;
import unknow.server.maven.AbstractMojo;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBodyParam;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxrs-generator", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JaxRsServlet extends AbstractMojo {
	private static final Logger logger = LoggerFactory.getLogger(JaxRsServlet.class);

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

	private TypeModel collection;
	private TypeModel set;
	private TypeModel sortedSet;

	private JaxrsModel model;

	private TypeCache types;
	private ClassOrInterfaceDeclaration cl;

	@Parameter(name = "openapi")
	private OpenApiBuilder openapi = new OpenApiBuilder();

	private final Map<JaxrsParam, String> converterVar = new HashMap<>();

	private BeanParamBuilder beans;

	@Override
	protected String id() {
		return "jaxrs-generator";
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();
		collection = loader.get(Collection.class.getCanonicalName());
		set = loader.get(Set.class.getCanonicalName());
		sortedSet = loader.get(SortedSet.class.getCanonicalName());
		model = new JaxrsModel(loader);
		processSrc(cu -> cu.walk(ClassOrInterfaceDeclaration.class, t -> model.process(loader.get(t.getFullyQualifiedName().get()).asClass())));
		model.implicitConstructor.remove("java.lang.String");

		beans = new BeanParamBuilder(loader, packageName, existingClass);

		try {
			generateInitalizer();
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}
		for (String path : model.paths())
			generateClass(path);

		out.save(openapi.build(project, model, packageName, existingClass));
		beans.save(out);
	}

	/**
	 * 
	 */

	private void generateInitalizer() throws IOException, MojoExecutionException {

		Path path = Paths.get(resources, "META-INF", "services", ServletContainerInitializer.class.getName());
		Files.createDirectories(path.getParent());
		try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			w.append(packageName).write(".JaxrsInit\n");
		}

		CompilationUnit cu = new CompilationUnit(packageName);
		cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);
		types = new TypeCache(cu, existingClass);

		cl = cu.addClass("JaxrsInit", Utils.PUBLIC).addImplementedType(ServletContainerInitializer.class);
		BlockStmt b = cl.addMethod("onStartup", Utils.PUBLIC).addMarkerAnnotation(Override.class)
				.addParameter(types.getClass(Set.class, types.getClass(Class.class, TypeCache.ANY)), "c").addParameter(types.getClass(ServletContext.class), "ctx").getBody()
				.get().addStatement(new MethodCallExpr(new TypeExpr(types.getClass(RuntimeDelegate.class)), "setInstance",
						Utils.list(new ObjectCreationExpr(null, types.getClass(JaxrsRuntime.class), Utils.list()))));
		for (String s : model.converter)
			b.addStatement(new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "registerConverter",
					Utils.list(new ObjectCreationExpr(null, types.getClass(s), Utils.list()))));
		if (!model.implicitConstructor.isEmpty() || !model.implicitFromString.isEmpty() || !model.implicitValueOf.isEmpty())
			b.addStatement(new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "registerConverter",
					Utils.list(new ObjectCreationExpr(null, new ClassOrInterfaceType(null, "P"), Utils.list()))));

		for (Entry<String, List<String>> e : model.readers.entrySet()) {
			NodeList<Expression> l = new NodeList<>(new ObjectCreationExpr(null, types.getClass(e.getKey()), Utils.list()));
			for (String s : e.getValue())
				l.add(Utils.text(s));
			b.addStatement(new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "registerReader", l));
		}
		for (Entry<String, List<String>> e : model.writers.entrySet()) {
			NodeList<Expression> l = new NodeList<>(new ObjectCreationExpr(null, types.getClass(e.getKey()), Utils.list()));
			for (String s : e.getValue())
				l.add(Utils.text(s));
			b.addStatement(new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "registerWriter", l));
		}

		generateImplicitConverter(cu);

		out.save(cu);

	}

	private void generateImplicitConverter(CompilationUnit cu) {
		if (model.implicitConstructor.isEmpty() && model.implicitFromString.isEmpty() && model.implicitValueOf.isEmpty())
			return;
		TypeParameter t = new TypeParameter("T");
		ClassOrInterfaceType p = types.getClass(ParamConverter.class, t);
		ClassOrInterfaceDeclaration clazz = cu.addClass("P", Modifier.Keyword.FINAL).addImplementedType(ParamConverterProvider.class);
		BlockStmt b = new BlockStmt();

		int i = 0;
		for (String c : model.implicitConstructor) {
			String n = "c$" + i++;
			generateImplicitConverter(clazz, n, c, null);
			b.addStatement(new IfStmt(new BinaryExpr(new ClassExpr(types.getClass(c)), new NameExpr("rawType"), BinaryExpr.Operator.EQUALS),
					new ReturnStmt(new CastExpr(p, new NameExpr(n))), null));
		}
		for (String c : model.implicitFromString) {
			String n = "c$" + i++;
			generateImplicitConverter(clazz, n, c, "fromString");
			b.addStatement(new IfStmt(new BinaryExpr(new ClassExpr(types.getClass(c)), new NameExpr("rawType"), BinaryExpr.Operator.EQUALS),
					new ReturnStmt(new CastExpr(p, new NameExpr(n))), null));
		}
		for (String c : model.implicitValueOf) {
			String n = "c$" + i++;
			generateImplicitConverter(clazz, n, c, "valueOf");
			b.addStatement(new IfStmt(new BinaryExpr(new ClassExpr(types.getClass(c)), new NameExpr("rawType"), BinaryExpr.Operator.EQUALS),
					new ReturnStmt(new CastExpr(p, new NameExpr(n))), null));
		}

		clazz.addMethod("getConverter", Utils.PUBLIC).addMarkerAnnotation(Override.class).addSingleMemberAnnotation(SuppressWarnings.class, Utils.text("unchecked"))
				.addTypeParameter(t).setType(p).addParameter(types.getClass(Class.class, t), "rawType").addParameter(types.getClass(Type.class), "genericType")
				.addParameter(types.array(Annotation.class), "Annotation").setBody(b.addStatement(new ReturnStmt(new NullLiteralExpr())));
	}

	private void generateImplicitConverter(ClassOrInterfaceDeclaration clazz, String name, String cl, String m) {
		ClassOrInterfaceType type = types.getClass(cl);
		Expression e;
		if (m == null)
			e = new ObjectCreationExpr(null, type, Utils.list(new NameExpr("value")));
		else
			e = new MethodCallExpr(new TypeExpr(type), m, Utils.list(new NameExpr("value")));

		NodeList<BodyDeclaration<?>> methods = Utils.list(
				new MethodDeclaration(Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL), Utils.list(new MarkerAnnotationExpr("Override")),
						Utils.list(), type, new SimpleName("fromString"), Utils.list(new com.github.javaparser.ast.body.Parameter(types.getClass(String.class), "value")),
						Utils.list(), new BlockStmt().addStatement(new ReturnStmt(e))),
				new MethodDeclaration(Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL), Utils.list(new MarkerAnnotationExpr("Override")),
						Utils.list(), types.getClass(String.class), new SimpleName("toString"), Utils.list(new com.github.javaparser.ast.body.Parameter(type, "value")),
						Utils.list(), new BlockStmt().addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr("value"), "toString")))));
		clazz.addFieldWithInitializer(types.getClass(ParamConverter.class, type), name,
				new ObjectCreationExpr(null, types.getClass(ParamConverter.class, TypeCache.EMPTY), null, Utils.list(), methods), Utils.PSF);
	}

	/**
	 * @param key
	 * @param value
	 * @throws MojoExecutionException
	 */
	private void generateClass(String path) throws MojoExecutionException {
		CompilationUnit cu = new CompilationUnit(packageName);
		cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);
		types = new TypeCache(cu, existingClass);

		cl = cu.addClass("Jaxrs" + path.replace('/', '_').replace("{}", ""), Utils.PUBLIC).addSingleMemberAnnotation(WebServlet.class, Utils.text(path))
				.addExtendedType(HttpServlet.class);

		cl.addFieldWithInitializer(long.class, "serialVersionUID", new LongLiteralExpr("1"), Utils.PSF);

		BlockStmt b = cl.addStaticInitializer().addStatement(new VariableDeclarationExpr(types.array(Type.class), "t"))
				.addStatement(new VariableDeclarationExpr(new ArrayType(types.array(Annotation.class)), "a"))
				.addStatement(new VariableDeclarationExpr(types.getClass(Type.class), "r")).addStatement(new VariableDeclarationExpr(types.array(Annotation.class), "ra"));

		Map<String, NameExpr> services = new HashMap<>();
		Set<MethodModel> methods = new HashSet<>();
		for (String method : model.methods(path)) {
			for (JaxrsMapping m : model.mapping(path, method)) {

				ClassModel c = m.clazz;
				methods.add(m.m);

				String n = "s$" + services.size();
				if (!services.containsKey(c.name())) {
					services.put(c.name(), new NameExpr(n));
					cl.addFieldWithInitializer(types.get(c.name()), n, new ObjectCreationExpr(null, types.getClass(c), Utils.list()), Utils.PSF);
				}

				List<ParamModel<MethodModel>> parameters = m.m.parameters();
				NodeList<Expression> classes = new NodeList<>();
				for (ParamModel<MethodModel> p : parameters)
					classes.add(new ClassExpr(types.get(p.type().name())));
				NodeList<Expression> getMethod = new NodeList<>();
				getMethod.add(Utils.text(m.m.name()));
				getMethod.addAll(classes);

				BlockStmt t = new BlockStmt()
						.addStatement(
								Utils.assign(types.getClass(Method.class), "m", new MethodCallExpr(new ClassExpr(types.get(m.m.parent().name())), "getMethod", getMethod)))
						.addStatement(new AssignExpr(new NameExpr("t"), new MethodCallExpr(new NameExpr("m"), "getGenericParameterTypes"), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("a"), new MethodCallExpr(new NameExpr("m"), "getParameterAnnotations"), AssignExpr.Operator.ASSIGN));
				BlockStmt ca = new BlockStmt().addStatement(new AssignExpr(new NameExpr("t"), Utils.array(types.getClass(Type.class), classes), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("a"), Utils.array(types.getClass(Annotation.class), parameters.size(), 0), AssignExpr.Operator.ASSIGN));

				if (!m.m.type().isVoid()) {
					t.addStatement(new AssignExpr(new NameExpr("r"), new MethodCallExpr(new NameExpr("m"), "getGenericReturnType"), AssignExpr.Operator.ASSIGN))
							.addStatement(new AssignExpr(new NameExpr("ra"), new MethodCallExpr(new NameExpr("m"), "getAnnotations"), AssignExpr.Operator.ASSIGN));
					ca.addStatement(new AssignExpr(new NameExpr("r"), new ClassExpr(types.get(m.m.type().name())), AssignExpr.Operator.ASSIGN))
							.addStatement(new AssignExpr(new NameExpr("ra"), Utils.array(types.getClass(Annotation.class), 0), AssignExpr.Operator.ASSIGN));
				}

				b.addStatement(new TryStmt(t, Utils.list(new CatchClause(new com.github.javaparser.ast.body.Parameter(types.getClass(Exception.class), "e"), ca)), null));

				int i = 0;
				for (JaxrsParam p : m.params)
					processConverter(p, m.var + "$" + i, i++, b);

				TypeModel type = m.m.type();
				if (!type.isVoid()) {
					if (type.isPrimitive())
						type = loader.get(type.asPrimitive().boxed());

					cl.addField(types.getClass(JaxrsEntityWriter.class, types.get(type)), m.var + "$r", Utils.PSF);
					b.addStatement(new AssignExpr(new NameExpr(m.var + "$r"), new MethodCallExpr(new TypeExpr(types.getClass(JaxrsEntityWriter.class)), "create",
							Utils.list(new ClassExpr(types.get(m.m.type().name())), new NameExpr("r"), new NameExpr("ra"))), AssignExpr.Operator.ASSIGN));
				}
				for (JaxrsParam p : m.params) {
					if (p instanceof JaxrsBeanParam)
						beans.add((JaxrsBeanParam) p);
				}
			}
		}

		NameExpr m = new NameExpr("m");
		Expression[] p = { new NameExpr("req"), new NameExpr("res") };
		IfStmt i = new IfStmt(new MethodCallExpr(Utils.text("TRACE"), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr("doTrace", p)),
				new ExpressionStmt(new MethodCallExpr(new NameExpr("res"), "sendError", Utils.list(new IntegerLiteralExpr("405")))));
		if (!model.methods(path).contains("OPTIONS"))
			i = new IfStmt(new MethodCallExpr(Utils.text("OPTIONS"), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr("doOptions", p)), i);
		if (!model.methods(path).contains("HEAD") && model.methods(path).contains("GET"))
			i = new IfStmt(new MethodCallExpr(Utils.text("HEAD"), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr("doHead", p)), i);
		for (String method : model.methods(path))
			i = new IfStmt(new MethodCallExpr(Utils.text(method), "equals", Utils.list(m)),
					new ExpressionStmt(new MethodCallExpr("do" + method.charAt(0) + method.substring(1).toLowerCase(), p)), i);

		cl.addMethod("service", Utils.PUBLIC).addMarkerAnnotation(Override.class).addParameter(types.getClass(HttpServletRequest.class), "req")
				.addParameter(types.getClass(HttpServletResponse.class), "res").addThrownException(IOException.class).addThrownException(ServletException.class).getBody()
				.get().addStatement(Utils.assign(types.getClass(String.class), "m", new MethodCallExpr(new NameExpr("req"), "getMethod"))).addStatement(i);

		if (!model.methods(path).contains("OPTIONS")) {
			StringBuilder sb = new StringBuilder("TRACE,OPTIONS");
			for (String s : model.methods(path))
				sb.append(',').append(s);
			if (!model.methods(path).contains("HEAD") && model.methods(path).contains("GET"))
				sb.append(",HEAD");

			cl.addMethod("doOptions", Utils.PROTECT).addMarkerAnnotation(Override.class).addParameter(types.getClass(HttpServletRequest.class), "req")
					.addParameter(types.getClass(HttpServletResponse.class), "res").addThrownException(IOException.class).addThrownException(ServletException.class).getBody()
					.get().addStatement(new MethodCallExpr(new NameExpr("res"), "setHeader", Utils.list(Utils.text("Allow"), Utils.text(sb.toString()))));
		}

		for (String method : model.methods(path))
			buildMethod(method, model.mapping(path, method));

		for (String method : model.methods(path)) {
			for (JaxrsMapping mapping : model.mapping(path, method))
				buildCall(mapping, services);
		}

		out.save(cu);
	}

	/**
	 * @param p
	 * @param string
	 * @param b
	 */
	private void processConverter(JaxrsParam p, String n, int i, BlockStmt b) {
		if (p instanceof JaxrsBeanParam)
			return;
		converterVar.put(p, n);
		TypeModel m = p.type.isPrimitive() ? loader.get(p.type.asPrimitive().boxed()) : p.type;
		if (p instanceof JaxrsBodyParam) {
			cl.addField(types.getClass(JaxrsEntityReader.class, types.get(m)), n, Utils.PSF);
			b.addStatement(new AssignExpr(new NameExpr(n),
					new ObjectCreationExpr(null, types.getClass(JaxrsEntityReader.class, TypeCache.EMPTY), Utils.list(new ClassExpr(types.get(p.type.name())),
							new ArrayAccessExpr(new NameExpr("t"), new IntegerLiteralExpr("" + i)), new ArrayAccessExpr(new NameExpr("a"), new IntegerLiteralExpr("" + i)))),
					AssignExpr.Operator.ASSIGN));
		} else {
			m = JaxrsModel.getParamType(m);
			cl.addField(types.getClass(ParamConverter.class, types.get(m.isPrimitive() ? m.asPrimitive().boxed() : m.name())), n, Utils.PSF);
			b.addStatement(new AssignExpr(new NameExpr(n),
					new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "converter", Utils.list(new ClassExpr(types.get(m.name())),
							new ArrayAccessExpr(new NameExpr("t"), new IntegerLiteralExpr("" + i)), new ArrayAccessExpr(new NameExpr("a"), new IntegerLiteralExpr("" + i)))),
					AssignExpr.Operator.ASSIGN));
		}
	}

	/**
	 * @param method
	 * @param mapping
	 */
	private void buildMethod(String method, List<JaxrsMapping> list) {
		BlockStmt b = new BlockStmt();
		cl.addMethod("do" + method.charAt(0) + method.substring(1).toLowerCase(), Utils.PROTECT).addMarkerAnnotation(Override.class)
				.addParameter(types.getClass(HttpServletRequest.class), "req").addParameter(types.getClass(HttpServletResponse.class), "res")
				.addThrownException(IOException.class).addThrownException(ServletException.class).getBody().get()
				.addStatement(Utils.create(types.getClass(JaxrsReq.class), "r", Utils.list(new NameExpr("req"))))
				.addStatement(new TryStmt(b,
						Utils.list(new CatchClause(new com.github.javaparser.ast.body.Parameter(types.getClass(Throwable.class), "e"),
								new BlockStmt().addStatement(new MethodCallExpr(null, "log", Utils.list(Utils.text("failed to process"), new NameExpr("e"))))
										.addStatement(new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "sendError",
												Utils.list(new NameExpr("r"), new NameExpr("e"), new NameExpr("res")))))),
						null));

		Map<String, Map<String, JaxrsMapping>> consume = new HashMap<>();
		for (JaxrsMapping mapping : list) {
			for (int i = 0; i < mapping.consume.length; i++) {
				String c = mapping.consume[i];
				Map<String, JaxrsMapping> map = consume.get(c);
				if (map == null)
					consume.put(c, map = new HashMap<>());

				for (int j = 0; j < mapping.produce.length; j++)
					map.put(mapping.produce[j], mapping);
			}
		}

		Map<String, JaxrsMapping> def = consume.remove("*/*");
		if (consume.isEmpty())
			buildProduces(b, def);
		else {
			b.addStatement(Utils.assign(types.getClass(String.class), "contentType", new MethodCallExpr(new NameExpr("r"), "getHeader",
					Utils.list(Utils.text("content-type"), Utils.text("*/*"), new FieldAccessExpr(new TypeExpr(types.getClass(DefaultConvert.class)), "STRING")))));
			List<String> k = new ArrayList<>(consume.keySet());
			k.sort(MIME);

			Statement stmt = def == null ? new ThrowStmt(new ObjectCreationExpr(null, types.getClass(NotSupportedException.class), Utils.list()))
					: buildProduces(new BlockStmt(), def);
			for (String s : k) {
				Map<String, JaxrsMapping> map = consume.get(s);
				String m = "equals";
				if (s.endsWith("/*")) {
					s = s.substring(0, s.length() - 1);
					m = "startsWith";
				} else if (s.startsWith("*/")) {
					s = s.substring(1);
					m = "endsWith";
				}

				stmt = new IfStmt(new MethodCallExpr(new NameExpr("contentType"), m, Utils.list(Utils.text(s))), buildProduces(new BlockStmt(), map), stmt);
			}
			b.addStatement(stmt);
		}
	}

	private Statement buildProduces(BlockStmt b, Map<String, JaxrsMapping> produce) {
		JaxrsMapping def = produce.remove("*/*");
		Statement stmt = new ThrowStmt(new ObjectCreationExpr(null, types.getClass(NotAcceptableException.class), Utils.list()));
		if (def != null)
			stmt = new ExpressionStmt(new MethodCallExpr(def.var + "$call", new NameExpr("r"), new NameExpr("res")));
		if (produce.size() == 0)
			return b.addStatement(stmt);

		// TODO quality check of header
		b.addStatement(Utils.assign(types.getClass(String.class), "accept", new MethodCallExpr(new NameExpr("r"), "getHeader",
				Utils.list(Utils.text("accept"), Utils.text("*/*"), new FieldAccessExpr(new TypeExpr(types.getClass(DefaultConvert.class)), "STRING")))));

		List<String> k = new ArrayList<>(produce.keySet());
		k.sort(MIME);
		for (String s : k) {
			stmt = new IfStmt(new MethodCallExpr(new NameExpr("accept"), "equals", Utils.list(Utils.text(s))),
					new ExpressionStmt(new MethodCallExpr(produce.get(s).var + "$call", new NameExpr("r"), new NameExpr("res"))), stmt);
		}
		b.addStatement(stmt);
		return b;
	}

	private void buildCall(JaxrsMapping mapping, Map<String, NameExpr> services) {
		NodeList<Expression> paths = mapping.parts.stream()
				.map(p -> new ObjectCreationExpr(null, types.getClass(JaxrsPath.class), Utils.list(new IntegerLiteralExpr("" + p.i), Utils.text(p.name))))
				.collect(Collectors.toCollection(NodeList::new));
		if (!mapping.parts.isEmpty())
			cl.addFieldWithInitializer(types.array(JaxrsPath.class), mapping.var + "$paths", Utils.array(types.getClass(JaxrsPath.class), paths), Utils.PSF);

		BlockStmt b = cl.addMethod(mapping.var + "$call", Utils.PSF).addParameter(types.getClass(JaxrsReq.class), "r")
				.addParameter(types.getClass(HttpServletResponse.class), "res").addThrownException(types.getClass(Exception.class)).getBody().get();
		if (!mapping.parts.isEmpty())
			b.addStatement(new MethodCallExpr(new NameExpr("r"), "initPaths", Utils.list(new NameExpr(mapping.var + "$paths"))));
		for (JaxrsParam p : mapping.params)
			b.addStatement(Utils.assign(types.get(p.type), p.var, getParam(p)));
		MethodModel m = mapping.m;
		NodeList<Expression> arg = mapping.params.stream().map(p -> new NameExpr(p.var)).collect(Collectors.toCollection(() -> new NodeList<>()));

		MethodCallExpr call = new MethodCallExpr(services.get(mapping.clazz.name()), m.name(), arg);
		if (m.type().isVoid()) {
			b.addStatement(call).addStatement(new MethodCallExpr(new NameExpr("res"), "sendError", Utils.list(new IntegerLiteralExpr("204"))));
		} else
			b.addStatement(Utils.assign(types.get(m.type()), "result", call))
					.addStatement(new MethodCallExpr(new NameExpr(mapping.var + "$r"), "write", Utils.list(new NameExpr("r"), new NameExpr("result"), new NameExpr("res"))));
	}

	/**
	 * @param key
	 * @param value
	 */

	private Expression getParam(JaxrsParam p) {
		if (p instanceof JaxrsBodyParam)
			return new MethodCallExpr(new NameExpr(converterVar.get(p)), "read", Utils.list(new NameExpr("r")));
		if (p instanceof JaxrsBeanParam)
			return new MethodCallExpr(new NameExpr("BeansReader"), beans.get((JaxrsBeanParam) p), Utils.list(new NameExpr("r")));

		String m = "get" + p.getClass().getSimpleName().substring(5, p.getClass().getSimpleName().length() - 5);
		if (p.type.isArray() || collection.isAssignableFrom(p.type))
			m += "Array";
		Expression e = new MethodCallExpr(new NameExpr("r"), m,
				Utils.list(Utils.text(p.value), p.def == null ? new NullLiteralExpr() : Utils.text(p.def), new NameExpr(converterVar.get(p))));
		if (collection.isAssignableFrom(p.type))
			e = new MethodCallExpr(new TypeExpr(types.get(Arrays.class)), "asList", Utils.list(e));
		if (sortedSet.isAssignableFrom(p.type))
			e = new ObjectCreationExpr(null, types.getClass(TreeSet.class), Utils.list(e));
		else if (set.isAssignableFrom(p.type))
			e = new ObjectCreationExpr(null, types.getClass(HashSet.class), Utils.list(e));
		return e;
	}
}
