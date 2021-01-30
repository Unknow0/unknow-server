package unknow.server.maven;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.printer.PrettyPrinterConfiguration;
import com.github.javaparser.printer.PrettyPrinterConfiguration.IndentType;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import picocli.CommandLine;
import unknow.server.http.HttpHandler;
import unknow.server.http.HttpRawProcessor;
import unknow.server.http.HttpRawRequest;
import unknow.server.http.PathMatcher;
import unknow.server.http.servlet.ArrayMap;
import unknow.server.http.servlet.EventManager;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletManager;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.nio.Handler;
import unknow.server.nio.HandlerFactory;
import unknow.server.nio.cli.NIOServerCli;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "servlet-generator")
public class ServletServerGen extends AbstractMojo {
	private static final Type EMPTY = new VarType();
	private static final List<Class<?>> LISTENERS = Arrays.asList(ServletContextListener.class, ServletContextAttributeListener.class, ServletRequestListener.class, ServletRequestAttributeListener.class, HttpSessionListener.class, HttpSessionAttributeListener.class, HttpSessionIdListener.class);

	private static final byte[] NOT_FOUND = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
	private static final byte[] ERROR = "HTTP/1.1 500 Server Error\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.UTF_8);

	private final CompilationUnit cu = new CompilationUnit();

	private TypeCache types;

	private JavaParser parser;

	private ClassOrInterfaceDeclaration cl;
	private ClassOrInterfaceType self;

	private List<SD> servlets = new ArrayList<>();
	private List<SD> filters = new ArrayList<>();
	private List<LD> listeners = new ArrayList<>();

	private final Set<String> existingClass = new HashSet<>();

	@Parameter(name = "src", defaultValue = "${project.build.sourceDirectory}")
	private String src;

	@Parameter(name = "className", defaultValue = "Server")
	private String className;

	@Parameter(name = "packageName")
	private String packageName = "unknow.server.http.test";

	@Parameter(name = "output")
	private String output;

	private String name = "ROOT";

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver(), new JavaParserTypeSolver(src)));
		parser = new JavaParser(new ParserConfiguration().setStoreTokens(true).setSymbolResolver(javaSymbolSolver));
		cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);

		if (output == null)
			output = src;

		try { // Collect annotation
			Files.walkFileTree(Paths.get(src), new W());
		} catch (IOException e) {
			throw new MojoFailureException("failed to get source failed", e);
		}
		// TODO read web.xml

		// generate class
		types = new TypeCache();
		generateClass();
	}

	private class W implements FileVisitor<Path>, Consumer<CompilationUnit> {
		private final Path local = Paths.get(src, packageName == null ? new String[0] : packageName.split("\\."));
		private final int count = local.getNameCount() + 1;

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (!file.getFileName().toString().endsWith(".java"))
				return FileVisitResult.CONTINUE;
			parser.parse(file).ifSuccessful(this);
			if (count == file.getNameCount() && file.startsWith(local)) {
				String string = file.getFileName().toString();
				existingClass.add(string.substring(0, string.length() - 5));
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public void accept(CompilationUnit c) {
			for (ClassOrInterfaceDeclaration t : c.findAll(ClassOrInterfaceDeclaration.class)) {
				Optional<AnnotationExpr> o = t.getAnnotationByClass(WebServlet.class);
				if (o.isPresent())
					servlets.add(new SD(servlets.size(), o.get(), t));
				// TODO collect other annotation
				o = t.getAnnotationByClass(WebListener.class);
				if (o.isPresent())
					processListener(t);

				o = t.getAnnotationByClass(WebFilter.class);
				if (o.isPresent())
					filters.add(new SD(servlets.size(), o.get(), t));
			}
		}

		private void processListener(ClassOrInterfaceDeclaration t) {
			Set<String> ancestror = new HashSet<>();
			for (ResolvedReferenceType i : t.resolve().getAllAncestors())
				ancestror.add(i.getQualifiedName());

			Set<Class<?>> listener = new HashSet<>();
			for (Class<?> cl : LISTENERS) {
				if (ancestror.contains(cl.getName()))
					listener.add(cl);
			}

			listeners.add(new LD(t, listener));
		}
	}

	private void generateClass() throws MojoFailureException {
		if (packageName != null && !packageName.isEmpty())
			cu.setPackageDeclaration(packageName);
		cl = cu.addClass(className, Modifier.Keyword.FINAL).addExtendedType(NIOServerCli.class).addImplementedType(HttpRawProcessor.class);
		self = type(cl);
		cl.addFieldWithInitializer(types.logger, "log", new MethodCallExpr(new TypeExpr(types.loggerFactory), "getLogger", list(new ClassExpr(self))));
		cl.addFieldWithInitializer(byte[].class, "NOT_FOUND", byteArray(NOT_FOUND), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
		cl.addFieldWithInitializer(byte[].class, "ERROR", byteArray(ERROR), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

		cl.addField(types.servletManager, "SERVLETS", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
		cl.addField(types.eventManager, "EVENTS", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
		cl.addField(types.servletContextImpl, "CTX", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

		constructor();

		createServletManager();
		createEventManager();
		createContext();

		loadInitializer();
		initialize();

		process();
		call();
		main();

		PrettyPrinterConfiguration pp = new PrettyPrinterConfiguration();
		pp.setIndentType(IndentType.TABS).setIndentSize(1);
		pp.setOrderImports(true).setSpaceAroundOperators(true);

		Path file = Paths.get(output);
		if (packageName != null)
			file = file.resolve(packageName.replace('.', '/'));

		try (BufferedWriter w = Files.newBufferedWriter(file.resolve(className + ".java"), StandardCharsets.UTF_8)) {
			w.write(cu.toString(pp));
		} catch (IOException e) {
			throw new MojoFailureException("failed to write output class", e);
		}
	}

	private void constructor() {
		cl.addConstructor(Modifier.Keyword.PRIVATE)
				.getBody()
				.addStatement(new AssignExpr(Names.EVENTS, new MethodCallExpr(new ThisExpr(), "createEventManager"), Operator.ASSIGN))
				.addStatement(new AssignExpr(Names.SERVLETS, new MethodCallExpr(new ThisExpr(), "createServletManager"), Operator.ASSIGN))
				.addStatement(new AssignExpr(Names.CTX, new MethodCallExpr(new ThisExpr(), "createContext"), Operator.ASSIGN));
	}

	private void createServletManager() {
		BlockStmt init = cl.addMethod("createServletManager", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).setType(types.servletManager).getBody().get();

		Map<String, SD> map = new HashMap<>();
		List<String> path = new ArrayList<>();
		SD def = null;
		NodeList<Expression> list = new NodeList<>();
		for (SD s : servlets) {
			list.add(new ObjectCreationExpr(null, type(s.e), emptyList()));
			for (String p : s.pattern) {
				if (p.equals("/") && def != null || map.containsKey(p))
					getLog().error("duplicate pattern for path '" + p + "'");
				else if (!p.equals("/")) {
					map.put(p, s);
					path.add(p);
				} else
					def = s;
			}
		}
		init.addStatement(new AssignExpr(new VariableDeclarationExpr(types.arrayServlet, "s"), array(types.servlet, list), Operator.ASSIGN));

		Collections.sort(path, (o1, o2) -> o1.length() - o2.length());
		list = new NodeList<>();
		for (String p : path) {
			SD sd = map.get(p);
			ClassOrInterfaceType mt;
			if (p.charAt(0) == '/' && p.endsWith("/*")) {
				mt = types.pathStart;
				p = p.substring(0, p.length() - 2);
			} else if (p.startsWith("*.")) {
				mt = types.pathEnd;
				p = p.substring(1);
			} else
				mt = types.pathEquals;

			ObjectCreationExpr m = new ObjectCreationExpr(null, mt, list(byteArray(p.getBytes(StandardCharsets.UTF_8))));
			ArrayAccessExpr s = new ArrayAccessExpr(Names.s, new IntegerLiteralExpr("" + sd.index));

			list.add(new ObjectCreationExpr(null, types.sEntry, list(m, s)));
		}
		init.addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.servletManager, list(Names.s, array(types.sEntry, list)))));
	}

	private void createEventManager() {
		BlockStmt init = cl.addMethod("createEventManager", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).setType(types.eventManager).getBody().get();
		Map<Class<?>, NodeList<Expression>> map = new HashMap<>();

		for (LD l : listeners) {
			ClassOrInterfaceType t = type(l.t);
			Expression e = new ObjectCreationExpr(null, t, emptyList());

			for (SD s : servlets) {
				if (s.e == l.t) {
					e = new CastExpr(t, new ArrayAccessExpr(new MethodCallExpr(Names.SERVLETS, "getServlets"), new IntegerLiteralExpr(Integer.toString(s.index))));
					break;
				}
			}
			for (Class<?> c : l.listener) {
				NodeList<Expression> ll = map.get(c);
				if (ll == null)
					map.put(c, ll = new NodeList<>());
				ll.add(e);
			}
		}

		NodeList<Expression> list = new NodeList<>();
		for (Class<?> l : LISTENERS) {
			Expression a;
			NodeList<Expression> ll = map.get(l);
			if (ll != null)
				a = new MethodCallExpr(new TypeExpr(types.arrays), "asList", ll);
			else
				a = new IntegerLiteralExpr("0");

			list.add(new ObjectCreationExpr(null, types.arrayListDiamon, list(a)));
		}
		init.addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.eventManager, list)));
	}

	private void createContext() {
		cl.addMethod("createContext", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).setType(types.servletContextImpl)
				.getBody().get()
				.addStatement(new AssignExpr(new VariableDeclarationExpr(types.arrayMapString, "initParam"), new ObjectCreationExpr(null, types.arrayMapDiamon, emptyList()), Operator.ASSIGN))

				// TODO get initParam & name from web.xml
				.addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.servletContextImpl, list(new StringLiteralExpr(name), Names.initParam, Names.SERVLETS, Names.EVENTS))));
	}

	private void loadInitializer() {
		cl.addMethod("loadInitializer", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).addThrownException(types.servletException)
				.getBody().get()
				.addStatement(new ForEachStmt(
						new VariableDeclarationExpr(types.servletContainerInitializer, "i"),
						new MethodCallExpr(new TypeExpr(types.serviceLoader), "load", list(new ClassExpr(types.servletContainerInitializer))),
						new BlockStmt()
								// TODO HandleTypes annotation ?
								.addStatement(new MethodCallExpr(Names.i, "onStartup", list(new NullLiteralExpr(), Names.CTX)))));
	}

	private void initialize() {
		Collections.sort(servlets, (a, b) -> a.loadOnStartup - b.loadOnStartup);

		BlockStmt b = cl.addMethod("initialize", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).addThrownException(types.servletException)
				.getBody().get()
				.addStatement(new AssignExpr(new VariableDeclarationExpr(types.arrayServlet, "s"), new MethodCallExpr(Names.SERVLETS, "getServlets"), Operator.ASSIGN));

		for (SD s : servlets) {
			NodeList<Expression> k = new NodeList<>();
			NodeList<Expression> v = new NodeList<>();
			for (Entry<String, String> e : s.param.entrySet()) {
				k.add(new StringLiteralExpr(e.getKey()));
				v.add(new StringLiteralExpr(e.getValue()));
			}
			ObjectCreationExpr p = new ObjectCreationExpr(null, types.arrayMapDiamon, list(array(types.str, k), array(types.str, v)));
			b.addStatement(new MethodCallExpr(new ArrayAccessExpr(Names.s, new IntegerLiteralExpr(Integer.toString(s.index))), "init", list(new ObjectCreationExpr(null, types.servletConfigImpl, list(new StringLiteralExpr(s.displayName), Names.CTX, p)))));
		}

	}

	private void process() {
		cl.addMethod("process", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL)
				.addAnnotation(Override.class).addThrownException(IOException.class)
				.addParameter(HttpRawRequest.class, "request").addParameter(OutputStream.class, "out")
				.getBody().get()
				.addStatement(new AssignExpr(new VariableDeclarationExpr(types.servletRequestImpl, "req"), new ObjectCreationExpr(null, types.servletRequestImpl, list(Names.CTX, Names.request)), Operator.ASSIGN))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireRequestInitialized", list(Names.req)))
				.addStatement(new AssignExpr(new VariableDeclarationExpr(types.filterChain, "s"), new MethodCallExpr(Names.SERVLETS, "find", list(Names.req)), Operator.ASSIGN))

				.addStatement(new IfStmt(
						new BinaryExpr(Names.s, new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
						new BlockStmt()
								.addStatement(new MethodCallExpr(Names.out, "write", list(new NameExpr("NOT_FOUND"))))
								.addStatement(new MethodCallExpr(Names.out, "close"))
								.addStatement(new MethodCallExpr(Names.EVENTS, "fireRequestDestroyed", list(new NameExpr("req"))))
								.addStatement(new ReturnStmt()),
						null))

				.addStatement(new AssignExpr(new VariableDeclarationExpr(types.servletResponse, "res"), new NullLiteralExpr(), Operator.ASSIGN))
				.addStatement(new TryStmt(
						new BlockStmt().addStatement(new MethodCallExpr(Names.s, "doFilter", list(Names.req, Names.res))),
						list(
								new CatchClause(new com.github.javaparser.ast.body.Parameter(types.exception, "e"), new BlockStmt()
										.addStatement(new MethodCallExpr(Names.log, "error", list(new StringLiteralExpr("failed to service '{}'"), Names.e, Names.s)))
										.addStatement(new MethodCallExpr(Names.out, "write", list(new NameExpr("ERROR")))))),
						null))

				.addStatement(new MethodCallExpr(Names.EVENTS, "fireRequestDestroyed", list(new NameExpr("req"))))
				.addStatement(new MethodCallExpr(Names.out, "close"));
	}

	private void call() {
		cl.addMethod("call", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL).setType(Integer.class).addAnnotation(Override.class).addThrownException(Exception.class)
				.getBody().get()
				.addStatement(new MethodCallExpr("loadInitializer"))
				.addStatement(new MethodCallExpr("initialize"))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireContextInitialized", list(Names.CTX)))
				.addStatement(new AssignExpr(new VariableDeclarationExpr(types.integer, "err"), new MethodCallExpr(new SuperExpr(), "call"), Operator.ASSIGN))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireContextDestroyed", list(Names.CTX)))
				.addStatement(new ReturnStmt(new NameExpr("err")));
	}

	private void main() {
		Expression a = parser.parseExpression("r->{Thread t=new Thread(r);t.setDaemon(true);return t; }").getResult().get();

		MethodDeclaration annonCreate = new MethodDeclaration(Modifier.createModifierList(Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL), types.handler, "create").addAnnotation(Override.class);
		annonCreate.getBody().get().addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.httpHandler, list(new NameExpr("executor"), new NameExpr("c")))));

		cl.addMethod("main", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
				.addParameter(String[].class, "arg")
				.getBody().get()
				.addStatement(new AssignExpr(new VariableDeclarationExpr(self, "c"), new ObjectCreationExpr(null, self, emptyList()), Operator.ASSIGN))
				.addStatement(new AssignExpr(new VariableDeclarationExpr(types.executorService, "executor"), new MethodCallExpr(new TypeExpr(types.executors), "newCachedThreadPool").addArgument(a), Operator.ASSIGN))
				.addStatement(new AssignExpr(new FieldAccessExpr(new NameExpr("c"), "handler"), new ObjectCreationExpr(null, types.handlerFactory, null, emptyList(), list(annonCreate)), Operator.ASSIGN))
				.addStatement(new MethodCallExpr(new TypeExpr(types.system), "exit").addArgument(new MethodCallExpr(new ObjectCreationExpr(null, types.cmdLine, list(new NameExpr("c"))), "execute").addArgument(new NameExpr("arg"))));
	}

	private ClassOrInterfaceType type(ClassOrInterfaceDeclaration decl) {
		ResolvedReferenceTypeDeclaration resolve = decl.resolve();
		cu.addImport(resolve.getQualifiedName());
		return new ClassOrInterfaceType(null, decl.getName(), null);
	}

	private static Expression byteArray(byte[] b) {
		NodeList<Expression> nodeList = new NodeList<>();
		for (int i = 0; i < b.length; i++)
			nodeList.add(new IntegerLiteralExpr(Byte.toString(b[i])));

		return array(PrimitiveType.byteType(), nodeList);
	}

	private static ArrayCreationExpr array(Type type, NodeList<Expression> init) {
		return new ArrayCreationExpr(type, list(new ArrayCreationLevel()), new ArrayInitializerExpr(init));
	}

	@SuppressWarnings("rawtypes")
	private static final NodeList EMPTYLIST = new NodeList<>();

	@SuppressWarnings("unchecked")
	private static <T extends Node> NodeList<T> emptyList() {
		return EMPTYLIST;
	}

	@SafeVarargs
	private static <T extends Node> NodeList<T> list(T... t) {
		return new NodeList<>(t);
	}

	private static class SD {
		final int index;
		final ClassOrInterfaceDeclaration e;

		String displayName;
		final List<String> pattern = new ArrayList<>();
		final Map<String, String> param = new HashMap<>();
		int loadOnStartup = -1;
		final List<String> servletNames = new ArrayList<>(0);
		final List<DispatcherType> dispatcher = new ArrayList<>(0);

		public SD(int index, AnnotationExpr a, ClassOrInterfaceDeclaration e) {
			this.index = index;
			this.e = e;
			String name = "";
			for (Node n : a.getChildNodes()) {
				if (!(n instanceof MemberValuePair))
					continue;
				MemberValuePair m = (MemberValuePair) n;
				String k = m.getName().getIdentifier();
				if ("value".equals(k) || "urlPatterns".equals(k))
					add(pattern, m.getValue());
				else if ("loadOnStartup".equals(k))
					loadOnStartup = m.getValue().asIntegerLiteralExpr().asNumber().intValue();
				else if ("initParams".equals(k)) {
					String key = m.getValue().asAnnotationExpr().findFirst(MemberValuePair.class, w -> "name".equals(w.getName().getIdentifier())).get().getValue().asStringLiteralExpr().getValue();
					String value = m.getValue().asAnnotationExpr().findFirst(MemberValuePair.class, w -> "value".equals(w.getName().getIdentifier())).get().getValue().asStringLiteralExpr().getValue();
					param.put(key, value);
				} else if ("name".equals(k) || "filterName".equals(k))
					name = m.getValue().asStringLiteralExpr().getValue();
				else if ("displayName".equals(k))
					displayName = m.getValue().asStringLiteralExpr().getValue();
				else if ("servletNames".equals(k))
					add(servletNames, m.getValue());
				else if ("dispatcherTypes".equals(k))
					parseDispatcher(m.getValue());
			}
			if (displayName == null)
				displayName = name;
		}

		private void parseDispatcher(Expression e) {
			List<Expression> list;
			if (e.isArrayInitializerExpr())
				list = e.asArrayInitializerExpr().getValues();
			else
				list = Arrays.asList(e);

			for (Expression value : list) {
				String n;
				if (value.isFieldAccessExpr())
					n = value.asFieldAccessExpr().getNameAsString();
				else
					n = value.asNameExpr().getNameAsString();
				dispatcher.add(DispatcherType.valueOf(n));
			}
		}

		private static void add(List<String> list, Expression e) {
			List<Expression> values;
			if (e.isStringLiteralExpr())
				values = Arrays.asList(e);
			else
				values = e.asArrayInitializerExpr().getValues();
			for (Expression v : values)
				list.add(v.asStringLiteralExpr().getValue());
		}
	}

	private static class LD {
		final ClassOrInterfaceDeclaration t;
		final Set<Class<?>> listener;

		public LD(ClassOrInterfaceDeclaration t, Set<Class<?>> listener) {
			this.t = t;
			this.listener = listener;
		}
	}

	private final class TypeCache {
		ClassOrInterfaceType arrays = type(Arrays.class);
		ClassOrInterfaceType cmdLine = type(CommandLine.class);
		ClassOrInterfaceType eventManager = type(EventManager.class);
		ClassOrInterfaceType exception = type(Exception.class);
		ClassOrInterfaceType executorService = type(ExecutorService.class);
		ClassOrInterfaceType executors = type(Executors.class);
		ClassOrInterfaceType filterChain = type(FilterChain.class);
		ClassOrInterfaceType handler = type(Handler.class);
		ClassOrInterfaceType handlerFactory = type(HandlerFactory.class);
		ClassOrInterfaceType httpHandler = type(HttpHandler.class);
		ClassOrInterfaceType integer = type(Integer.class);
		ClassOrInterfaceType logger = type(Logger.class);
		ClassOrInterfaceType loggerFactory = type(LoggerFactory.class);
		ClassOrInterfaceType str = type(String.class);
		ClassOrInterfaceType sEntry = type(ServletManager.SEntry.class);
		ClassOrInterfaceType servlet = type(Servlet.class);
		ClassOrInterfaceType servletConfigImpl = type(ServletConfigImpl.class);
		ClassOrInterfaceType servletContextImpl = type(ServletContextImpl.class);
		ClassOrInterfaceType servletContainerInitializer = type(ServletContainerInitializer.class);
		ClassOrInterfaceType servletException = type(ServletException.class);
		ClassOrInterfaceType servletManager = type(ServletManager.class);
		ClassOrInterfaceType servletRequest = type(ServletRequest.class);
		ClassOrInterfaceType servletRequestImpl = type(ServletRequestImpl.class);
		ClassOrInterfaceType servletResponse = type(ServletResponse.class);
		ClassOrInterfaceType serviceLoader = type(ServiceLoader.class);
		ClassOrInterfaceType system = type(System.class);

		ClassOrInterfaceType pathStart = type(PathMatcher.StartMatcher.class);
		ClassOrInterfaceType pathEnd = type(PathMatcher.EndMatcher.class);
		ClassOrInterfaceType pathEquals = type(PathMatcher.ExactMatcher.class);

		ArrayType arrayServlet = new ArrayType(servlet);

		ClassOrInterfaceType arrayMapDiamon = type(ArrayMap.class, EMPTY);
		ClassOrInterfaceType arrayMapString = type(ArrayMap.class, str);
		ClassOrInterfaceType arrayListDiamon = type(ArrayList.class, EMPTY);

		@SuppressWarnings("null")
		private ClassOrInterfaceType type(Class<?> cl, Type... param) {
			ClassOrInterfaceType r = null;
			if (existingClass.contains(cl.getSimpleName())) { // need to fully qualify
				for (String s : cl.getName().split("\\.")) {
					r = new ClassOrInterfaceType(r, s);
				}
			} else {
				cu.addImport(cl);
				r = new ClassOrInterfaceType(null, cl.getSimpleName());
			}
			NodeList<Type> p = null;
			if (param.length > 0) {
				p = new NodeList<>();
				for (int i = 0; i < param.length; i++) {
					Type t = param[i];
					if (t != EMPTY)
						p.add(t);
				}
			}
			return r.setTypeArguments(p);
		}
	}

	public static final class Names {
		static final NameExpr CTX = new NameExpr("CTX");
		static final NameExpr SERVLETS = new NameExpr("SERVLETS");
		static final NameExpr EVENTS = new NameExpr("EVENTS");
		static final NameExpr log = new NameExpr("log");

		static final NameExpr initParam = new NameExpr("initParam");
		static final NameExpr i = new NameExpr("i");
		static final NameExpr e = new NameExpr("e");
		static final NameExpr s = new NameExpr("s");

		static final NameExpr out = new NameExpr("out");
		static final NameExpr request = new NameExpr("request");
		static final NameExpr req = new NameExpr("req");
		static final NameExpr res = new NameExpr("res");
	}
}