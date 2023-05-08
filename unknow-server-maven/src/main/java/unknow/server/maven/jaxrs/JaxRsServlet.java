/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
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

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.RuntimeDelegate;
import unknow.server.http.jaxrs.JaxrsContext;
import unknow.server.http.jaxrs.JaxrsEntityReader;
import unknow.server.http.jaxrs.JaxrsEntityWriter;
import unknow.server.http.jaxrs.JaxrsPath;
import unknow.server.http.jaxrs.JaxrsReq;
import unknow.server.http.jaxrs.JaxrsRuntime;
import unknow.server.maven.AbstractMojo;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBodyParam;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxrs-generator")
public class JaxRsServlet extends AbstractMojo {

	private static final Modifier.Keyword[] PUBLIC = { Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL };
	private static final Modifier.Keyword[] PROTECT = { Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL };
	private static final Modifier.Keyword[] PSF = { Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL };

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

	private TypeModel response;

	private JaxrsModel model;

	private TypeCache types;
	private ClassOrInterfaceDeclaration cl;

	@Parameter(name = "resources", defaultValue = "${project.build.directory}/jaxrs-generator")
	private String resources;

	private final Map<JaxrsParam, String> converterVar = new HashMap<>();

	private final Map<String, JaxrsBeanParam> beans = new HashMap<>();
	private final Map<String, String> beansVar = new HashMap<>();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();
		response = loader.get(Response.class.getName());
		model = new JaxrsModel(loader);
		processSrc(cu -> cu.walk(ClassOrInterfaceDeclaration.class, t -> model.process(loader.get(t.getFullyQualifiedName().get()).asClass())));

		try {
			generateInitalizer();
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}
		for (String path : model.paths())
			generateClass(path);
	}

	private void generateInitalizer() throws IOException, MojoExecutionException {
		Resource resource = new Resource();
		resource.setDirectory(resources);
		project.addResource(resource);

		Path path = Paths.get(resources, "META-INF", "services", ServletContainerInitializer.class.getName());
		Files.createDirectories(path.getParent());
		try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			w.write(packageName + ".JaxrsInit\n");
		}

		CompilationUnit cu = new CompilationUnit(packageName);
		cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);
		types = new TypeCache(cu, existingClass);

		cl = cu.addClass("JaxrsInit", PUBLIC)
				.addImplementedType(ServletContainerInitializer.class);
		BlockStmt b = cl.addMethod("onStartup", PUBLIC)
				.addMarkerAnnotation(Override.class)
				.addParameter(types.get(Set.class, types.get(Class.class, TypeCache.ANY)), "c")
				.addParameter(types.get(ServletContext.class), "ctx")
				.getBody().get()
				.addStatement(new MethodCallExpr(new TypeExpr(types.get(RuntimeDelegate.class)), "setInstance", Utils.list(new ObjectCreationExpr(null, types.get(JaxrsRuntime.class), Utils.list()))));
		for (String s : model.converter)
			b.addStatement(new MethodCallExpr(new TypeExpr(types.get(JaxrsContext.class)), "registerConverter", Utils.list(new ObjectCreationExpr(null, types.get(s), Utils.list()))));

		for (Entry<String, List<String>> e : model.readers.entrySet()) {
			NodeList<Expression> l = new NodeList<>(new ObjectCreationExpr(null, types.get(e.getKey()), Utils.list()));
			for (String s : e.getValue())
				l.add(new StringLiteralExpr(s));
			b.addStatement(new MethodCallExpr(new TypeExpr(types.get(JaxrsContext.class)), "registerReader", l));
		}
		for (Entry<String, List<String>> e : model.writers.entrySet()) {
			NodeList<Expression> l = new NodeList<>(new ObjectCreationExpr(null, types.get(e.getKey()), Utils.list()));
			for (String s : e.getValue())
				l.add(new StringLiteralExpr(s));
			b.addStatement(new MethodCallExpr(new TypeExpr(types.get(JaxrsContext.class)), "registerWriter", l));
		}

		out.save(cu);
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

		cl = cu.addClass("Jaxrs" + path.replace('/', '_').replaceAll("\u0000", ""), PUBLIC)
				.addSingleMemberAnnotation(WebServlet.class, new StringLiteralExpr(path.replace("\u0000", "\\u0000")))
				.addExtendedType(HttpServlet.class);

		cl.addFieldWithInitializer(long.class, "serialVersionUID", new LongLiteralExpr("1"), PSF);

		Map<ClassModel, NameExpr> services = new HashMap<>();
		Set<MethodModel> methods = new HashSet<>();
		for (String method : model.methods(path)) {
			for (JaxrsMapping mapping : model.mapping(path, method)) {

				ClassModel c = mapping.clazz;
				methods.add(mapping.m);

				String n = "s$" + services.size();
				if (services.containsKey(c))
					continue;
				services.put(c, new NameExpr(n));
				cl.addFieldWithInitializer(types.get(c.name()), n, new ObjectCreationExpr(null, types.get(c.name()), Utils.list()), PSF);
			}
		}

		BlockStmt b = new BlockStmt()
				.addStatement(new VariableDeclarationExpr(types.array(Type.class), "t"))
				.addStatement(new VariableDeclarationExpr(new ArrayType(types.array(Annotation.class)), "a"))
				.addStatement(new VariableDeclarationExpr(types.get(Type.class), "r"))
				.addStatement(new VariableDeclarationExpr(types.array(Annotation.class), "ra"));
		BlockStmt bean = new BlockStmt()
				.addStatement(new VariableDeclarationExpr(types.get(Type.class), "t"))
				.addStatement(new VariableDeclarationExpr(types.array(Annotation.class), "a"));

		for (JaxrsMapping m : model.mappings()) {
			List<ParamModel> parameters = m.m.parameters();
			NodeList<Expression> classes = new NodeList<>();
			for (ParamModel p : parameters)
				classes.add(new ClassExpr(types.get(p.type())));
			NodeList<Expression> getMethod = new NodeList<>();
			getMethod.add(new StringLiteralExpr(m.m.name()));
			getMethod.addAll(classes);

			BlockStmt t = new BlockStmt()
					.addStatement(Utils.assign(types.get(Method.class), "m", new MethodCallExpr(new ClassExpr(types.get(m.m.parent())), "getMethod", getMethod)))
					.addStatement(new AssignExpr(new NameExpr("t"), new MethodCallExpr(new NameExpr("m"), "getGenericParameterTypes"), AssignExpr.Operator.ASSIGN))
					.addStatement(new AssignExpr(new NameExpr("a"),
							new MethodCallExpr(new NameExpr("m"), "getParameterAnnotations"), AssignExpr.Operator.ASSIGN));
			BlockStmt c = new BlockStmt()
					.addStatement(new AssignExpr(new NameExpr("t"), Utils.array(types.get(Type.class), classes), AssignExpr.Operator.ASSIGN))
					.addStatement(new AssignExpr(new NameExpr("a"), Utils.array(types.get(Annotation.class), parameters.size(), 0), AssignExpr.Operator.ASSIGN));

			if (!m.m.type().isVoid()) {
				t.addStatement(new AssignExpr(new NameExpr("r"), new MethodCallExpr(new NameExpr("m"), "getGenericReturnType"), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("ra"), new MethodCallExpr(new NameExpr("m"), "getAnnotations"), AssignExpr.Operator.ASSIGN));
				c.addStatement(new AssignExpr(new NameExpr("r"), new ClassExpr(types.get(m.m.type())), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("ra"), Utils.array(types.get(Annotation.class), 0), AssignExpr.Operator.ASSIGN));
			}

			b.addStatement(new TryStmt(t,
					Utils.list(new CatchClause(
							new com.github.javaparser.ast.body.Parameter(types.get(Exception.class), "e"),
							c)),
					null));

			int i = 0;
			for (JaxrsParam p : m.params)
				processConverter(p, m.var + "$" + i, i++, b);

			if (!m.m.type().isVoid()) {
				cl.addField(types.get(JaxrsEntityWriter.class, types.get(m.m.type())), m.var + "$r", PSF);
				b.addStatement(new AssignExpr(new NameExpr(m.var + "$r"), new MethodCallExpr(new TypeExpr(types.get(JaxrsEntityWriter.class)), "create", Utils.list(
						new ClassExpr(types.get(m.m.type())),
						new NameExpr("r"),
						new NameExpr("ra"))), AssignExpr.Operator.ASSIGN));
			}
			for (JaxrsParam p : m.params) {
				if (!(p instanceof JaxrsBeanParam))
					continue;
				processBeanConverter((JaxrsBeanParam) p, bean);
			}
		}
		if (b.getChildNodes().size() > 2)
			cl.getMembers().add(new InitializerDeclaration(true, b));
		if (bean.getChildNodes().size() > 2)
			cl.getMembers().add(new InitializerDeclaration(true, bean));

		NameExpr m = new NameExpr("m");
		Expression[] p = { new NameExpr("req"), new NameExpr("res") };
		IfStmt i = new IfStmt(new MethodCallExpr(new StringLiteralExpr("TRACE"), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr("doTrace", p)), new ExpressionStmt(new MethodCallExpr(new NameExpr("res"), "sendError", Utils.list(new IntegerLiteralExpr("405")))));
		if (!model.methods(path).contains("OPTIONS"))
			i = new IfStmt(new MethodCallExpr(new StringLiteralExpr("OPTIONS"), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr("doOptions", p)), i);
		if (!model.methods(path).contains("HEAD") && model.methods(path).contains("GET"))
			i = new IfStmt(new MethodCallExpr(new StringLiteralExpr("HEAD"), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr("doHead", p)), i);
		for (String method : model.methods(path))
			i = new IfStmt(new MethodCallExpr(new StringLiteralExpr(method), "equals", Utils.list(m)), new ExpressionStmt(new MethodCallExpr("do" + method.charAt(0) + method.substring(1).toLowerCase(), p)), i);

		cl.addMethod("service", PUBLIC).addMarkerAnnotation(Override.class)
				.addParameter(types.get(HttpServletRequest.class), "req")
				.addParameter(types.get(HttpServletResponse.class), "res")
				.addThrownException(IOException.class).addThrownException(ServletException.class)
				.getBody().get()
				.addStatement(Utils.assign(types.get(String.class), "m", new MethodCallExpr(new NameExpr("req"), "getMethod")))
				.addStatement(i);

		if (!model.methods(path).contains("OPTIONS")) {
			StringBuilder sb = new StringBuilder("TRACE,OPTIONS");
			for (String s : model.methods(path))
				sb.append(',').append(s);
			cl.addMethod("doOptions", PROTECT).addMarkerAnnotation(Override.class)
					.addParameter(types.get(HttpServletRequest.class), "req")
					.addParameter(types.get(HttpServletResponse.class), "res")
					.addThrownException(IOException.class).addThrownException(ServletException.class)
					.getBody().get()
					.addStatement(new MethodCallExpr(new NameExpr("res"), "setHeader", Utils.list(new StringLiteralExpr("Allow"), new StringLiteralExpr(sb.toString()))));
		}

		for (String method : model.methods(path))
			buildMethod(method, model.mapping(path, method));

		for (JaxrsMapping mapping : model.mappings())
			buildCall(mapping, services);

		for (Entry<String, JaxrsBeanParam> e : beans.entrySet())
			buildBeanMethod(e.getKey(), e.getValue());
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
			cl.addField(types.get(JaxrsEntityReader.class, types.get(m)), n, PSF);
			b.addStatement(new AssignExpr(new NameExpr(n), new ObjectCreationExpr(null, types.get(JaxrsEntityReader.class, TypeCache.EMPTY), Utils.list(
					new ClassExpr(types.get(p.type)),
					new ArrayAccessExpr(new NameExpr("t"), new IntegerLiteralExpr("" + i)),
					new ArrayAccessExpr(new NameExpr("a"), new IntegerLiteralExpr("" + i)))), AssignExpr.Operator.ASSIGN));
		} else {
			cl.addField(types.get(ParamConverter.class, types.get(m)), n, PSF);
			b.addStatement(new AssignExpr(new NameExpr(n), new MethodCallExpr(new TypeExpr(types.get(JaxrsContext.class)), "converter", Utils.list(
					new ClassExpr(types.get(p.type)),
					new ArrayAccessExpr(new NameExpr("t"), new IntegerLiteralExpr("" + i)),
					new ArrayAccessExpr(new NameExpr("a"), new IntegerLiteralExpr("" + i)))), AssignExpr.Operator.ASSIGN));
		}
	}

	private void processBeanConverter(JaxrsBeanParam param, BlockStmt b) {
		if (beans.containsKey(param.clazz.name()))
			return;
		String n = "bean$" + beansVar.size();
		beans.put(param.clazz.name(), param);
		beansVar.put(param.clazz.name(), "buildBean" + beansVar.size());
		int i = 0;
		for (JaxrsParam p : param.fields.values())
			processBeanConverter(p, n + "$" + i++, b);
		for (JaxrsParam p : param.setters.values())
			processBeanConverter(p, n + "$" + i++, b);

	}

	private void processBeanConverter(JaxrsParam p, String n, BlockStmt b) {
		if (p instanceof JaxrsBeanParam) {
			processBeanConverter((JaxrsBeanParam) p, b);
			return;
		}
		converterVar.put(p, n);
		b.addStatement(new TryStmt(
				new BlockStmt()
						.addStatement(Utils.assign(types.get(Field.class), "f", new MethodCallExpr(new ClassExpr(types.get(p.parent)), "getDeclaredField", Utils.list(new StringLiteralExpr(p.name)))))
						.addStatement(new AssignExpr(new NameExpr("t"), new MethodCallExpr(new NameExpr("f"), "getGenericType"), AssignExpr.Operator.ASSIGN))
						.addStatement(new AssignExpr(new NameExpr("a"), new MethodCallExpr(new NameExpr("f"), "getAnnotations"), AssignExpr.Operator.ASSIGN)),
				Utils.list(new CatchClause(
						new com.github.javaparser.ast.body.Parameter(types.get(Exception.class), "e"),
						new BlockStmt()
								.addStatement(new AssignExpr(new NameExpr("t"), new ClassExpr(types.get(p.type)), AssignExpr.Operator.ASSIGN))
								.addStatement(new AssignExpr(new NameExpr("a"), Utils.array(types.get(Annotation.class), 0), AssignExpr.Operator.ASSIGN)))),
				null));

		if (p instanceof JaxrsBodyParam) {
			cl.addField(types.get(JaxrsEntityReader.class, types.get(p.type)), n, PSF);
			b.addStatement(new AssignExpr(new NameExpr(n), new MethodCallExpr(new TypeExpr(types.get(JaxrsContext.class)), "reader", Utils.list(
					new ClassExpr(types.get(p.type)),
					new NameExpr("t"),
					new NameExpr("a"),
					new NullLiteralExpr())), AssignExpr.Operator.ASSIGN));
		} else {
			cl.addField(types.get(ParamConverter.class, types.get(p.type)), n, PSF);
			b.addStatement(new AssignExpr(new NameExpr(n), new MethodCallExpr(new TypeExpr(types.get(JaxrsContext.class)), "converter", Utils.list(
					new ClassExpr(types.get(p.type)),
					new NameExpr("t"),
					new NameExpr("a"))), AssignExpr.Operator.ASSIGN));
		}
	}

	/**
	 * @param method
	 * @param mapping
	 */
	private void buildMethod(String method, List<JaxrsMapping> list) {
		BlockStmt b = new BlockStmt();
		cl.addMethod("do" + method.charAt(0) + method.substring(1).toLowerCase(), PROTECT).addMarkerAnnotation(Override.class)
				.addParameter(types.get(HttpServletRequest.class), "req")
				.addParameter(types.get(HttpServletResponse.class), "res")
				.addThrownException(IOException.class).addThrownException(ServletException.class)
				.getBody().get()
				.addStatement(Utils.create(types.get(JaxrsReq.class), "r", Utils.list(new NameExpr("req"))))
				.addStatement(new TryStmt(b, Utils.list(
						new CatchClause(
								new com.github.javaparser.ast.body.Parameter(types.get(Throwable.class), "e"),
								new BlockStmt()
										.addStatement(new MethodCallExpr(null, "log", Utils.list(new StringLiteralExpr("failed to process"), new NameExpr("e"))))
										.addStatement(new MethodCallExpr(new TypeExpr(types.get(JaxrsContext.class)), "sendError", Utils.list(new NameExpr("r"), new NameExpr("e"), new NameExpr("res")))))),
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
			b.addStatement(Utils.assign(types.get(String.class), "contentType", new MethodCallExpr(new NameExpr("r"), "getHeader", Utils.list(new StringLiteralExpr("content-type"), new StringLiteralExpr("*/*"), new FieldAccessExpr(new TypeExpr(types.get(JaxrsContext.class)), "STRING")))));
			List<String> k = new ArrayList<>(consume.keySet());
			k.sort(MIME);

			Statement stmt = def == null ? new ThrowStmt(new ObjectCreationExpr(null, types.get(NotSupportedException.class), Utils.list())) : buildProduces(new BlockStmt(), def);
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

				stmt = new IfStmt(
						new MethodCallExpr(new NameExpr("contentType"), m, Utils.list(new StringLiteralExpr(s))),
						buildProduces(new BlockStmt(), map),
						stmt);
			}
			b.addStatement(stmt);
		}
	}

	private Statement buildProduces(BlockStmt b, Map<String, JaxrsMapping> produce) {
		JaxrsMapping def = produce.remove("*/*");
		Statement stmt = new ThrowStmt(new ObjectCreationExpr(null, types.get(NotAcceptableException.class), Utils.list()));
		if (def != null)
			stmt = new ExpressionStmt(new MethodCallExpr(def.var + "$call", new NameExpr("r"), new NameExpr("res")));
		if (produce.size() == 0)
			return b.addStatement(stmt);

		// TODO quality check of header
		b.addStatement(Utils.assign(types.get(String.class), "accept", new MethodCallExpr(new NameExpr("r"), "getHeader", Utils.list(new StringLiteralExpr("accept"), new StringLiteralExpr("*/*"), new FieldAccessExpr(new TypeExpr(types.get(JaxrsContext.class)), "STRING")))));

		List<String> k = new ArrayList<>(produce.keySet());
		k.sort(MIME);
		for (String s : k) {
			stmt = new IfStmt(
					new MethodCallExpr(new NameExpr("accept"), "equals", Utils.list(new StringLiteralExpr(s))),
					new ExpressionStmt(new MethodCallExpr(produce.get(s).var + "$call", new NameExpr("r"), new NameExpr("res"))),
					stmt);
		}
		b.addStatement(stmt);
		return b;
	}

	private void buildCall(JaxrsMapping mapping, Map<ClassModel, NameExpr> services) {
		NodeList<Expression> paths = mapping.parts.stream().map(p -> new ObjectCreationExpr(null, types.get(JaxrsPath.class), Utils.list(new IntegerLiteralExpr("" + p.i), new StringLiteralExpr(p.name)))).collect(Collectors.toCollection(NodeList::new));
		cl.addFieldWithInitializer(types.array(JaxrsPath.class), mapping.var + "$paths", Utils.array(types.get(JaxrsPath.class), paths), PSF);

		BlockStmt b = cl.addMethod(mapping.var + "$call", PSF)
				.addParameter(types.get(JaxrsReq.class), "r").addParameter(types.get(HttpServletResponse.class), "res")
				.addThrownException(types.get(IOException.class))
				.getBody().get();
		if (!mapping.parts.isEmpty())
			b.addStatement(new MethodCallExpr(new NameExpr("r"), "initPaths", Utils.list(new NameExpr(mapping.var + "$paths"))));
		for (JaxrsParam p : mapping.params)
			b.addStatement(Utils.assign(types.get(p.type), p.var, getParam(p)));
		MethodModel m = mapping.m;
		NodeList<Expression> arg = mapping.params.stream().map(p -> new NameExpr(p.var)).collect(Collectors.toCollection(() -> new NodeList<>()));

		MethodCallExpr call = new MethodCallExpr(services.get(mapping.clazz), m.name(), arg);
		if (m.type().isVoid()) {
			b.addStatement(call)
					.addStatement(new MethodCallExpr(new NameExpr("res"), "sendError", Utils.list(new IntegerLiteralExpr("204"))));
		} else if (m.type().isAssignableFrom(response)) {
			b.addStatement(Utils.assign(types.get(m.type()), "result", call))
					// TODO set response
					.addStatement(new MethodCallExpr(new NameExpr(mapping.var + "$r"), "write", Utils.list(new NameExpr("r"), new MethodCallExpr(new NameExpr("result"), "getEntity"), new NameExpr("res"))));
		} else
			b.addStatement(Utils.assign(types.get(m.type()), "result", call))
					.addStatement(new MethodCallExpr(new NameExpr(mapping.var + "$r"), "write", Utils.list(new NameExpr("r"), new NameExpr("result"), new NameExpr("res"))));
	}

	/**
	 * @param key
	 * @param value
	 */
	private void buildBeanMethod(String clazz, JaxrsBeanParam bean) {
		BlockStmt b = cl.addMethod(beansVar.get(clazz), PSF).addParameter(types.get(JaxrsReq.class), "r")
				.setType(types.get(clazz))
				.getBody().get()
				.addStatement(Utils.create(types.get(clazz), "b", Utils.list()));
		for (Entry<FieldModel, JaxrsParam> e : bean.fields.entrySet())
			b.addStatement(new AssignExpr(new FieldAccessExpr(new NameExpr("b"), e.getKey().name()), getParam(e.getValue()), AssignExpr.Operator.ASSIGN));
		for (Entry<MethodModel, JaxrsParam> e : bean.setters.entrySet())
			b.addStatement(new MethodCallExpr(new NameExpr("b"), e.getKey().name(), Utils.list(getParam(e.getValue()))));
		b.addStatement(new ReturnStmt(new NameExpr("b")));
	}

	private Expression getParam(JaxrsParam p) {
		if (p instanceof JaxrsBodyParam)
			return new MethodCallExpr(new NameExpr(converterVar.get(p)), "read", Utils.list(new NameExpr("r")));
		if (p instanceof JaxrsBeanParam)
			return new MethodCallExpr(beansVar.get(((JaxrsBeanParam) p).clazz.name()), new NameExpr("r"));

		String m = "get" + p.getClass().getSimpleName().substring(5, p.getClass().getSimpleName().length() - 5);
		return new MethodCallExpr(new NameExpr("r"), m, Utils.list(new StringLiteralExpr(p.value), p.def == null ? new NullLiteralExpr() : new StringLiteralExpr(p.def), new NameExpr(converterVar.get(p))));
	}
}
