package unknow.server.maven.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import picocli.CommandLine.Option;
import unknow.sax.SaxParser;
import unknow.server.http.HttpRawProcessor;
import unknow.server.http.servlet.DefaultServlet;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.Resource;
import unknow.server.http.utils.ServletManager;
import unknow.server.maven.Output;
import unknow.server.maven.TypeCache;
import unknow.server.maven.servlet.Builder.BuilderContext;
import unknow.server.maven.servlet.builder.Call;
import unknow.server.maven.servlet.builder.Constructor;
import unknow.server.maven.servlet.builder.CreateContext;
import unknow.server.maven.servlet.builder.CreateEventManager;
import unknow.server.maven.servlet.builder.CreateServletManager;
import unknow.server.maven.servlet.builder.Initialize;
import unknow.server.maven.servlet.builder.LoadInitializer;
import unknow.server.maven.servlet.builder.Main;
import unknow.server.maven.servlet.builder.Process;
import unknow.server.maven.servlet.descriptor.Descriptor;
import unknow.server.maven.servlet.descriptor.SD;
import unknow.server.maven.servlet.sax.Context;
import unknow.server.nio.cli.NIOServerCli;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "servlet-generator")
public class ServletServerGen extends AbstractMojo implements BuilderContext {
	private static final List<Builder> BUILDER = Arrays.asList(new Constructor(), new CreateEventManager(), new CreateServletManager(), new CreateContext(), new LoadInitializer(), new Initialize(), new Process(), new Call(), new Main());

	private final CompilationUnit cu = new CompilationUnit();

	private TypeCache types;

	private JavaParser parser;

	private ClassOrInterfaceDeclaration cl;

	private final Descriptor descriptor = new Descriptor();

	@Parameter(name = "src", defaultValue = "${project.build.sourceDirectory}")
	private String src;

	@Parameter(name = "className", defaultValue = "Server")
	private String className;

	@Parameter(name = "packageName")
	private String packageName;

	@Parameter(name = "output")
	private String output;

	@Parameter(name = "web-xml", defaultValue = "${project.basedir}/src/main/resources/WEB-INF/web.xml")
	private String webXml;

	@Parameter(name = "resources", defaultValue = "${project.basedir}/src/main/resources")
	private String resources;

	@Parameter(name = "sessionFactory", defaultValue = "unknow.server.http.servlet.session.NoSessionFactory")
	private String sessionFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		TypeSolver resolver = new CombinedTypeSolver(new ReflectionTypeSolver(), new JavaParserTypeSolver(src));
		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(resolver);
		parser = new JavaParser(new ParserConfiguration().setStoreTokens(true).setSymbolResolver(javaSymbolSolver));
		cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);

		if (output == null)
			output = src;

		if (webXml != null) {
			try {
				try (InputStream r = Files.newInputStream(Paths.get(webXml))) {
					SaxParser.parse(new Context(descriptor, resolver), new InputSource(r));
				}
			} catch (Exception e) {
				getLog().error("failed to parse '" + webXml + "'", e);
			}
		}
		try { // collecting resources files
			Path path = Paths.get(resources);
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					dir = path.relativize(dir);
					String path = dir.getName(0).toString().toUpperCase();
					return "WEB-INF".equals(path) || "META-INF".equals(path) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					descriptor.resources.put("/" + path.relativize(file).toString().replace('\\', '/'), new Resource(Files.getLastModifiedTime(file).to(TimeUnit.MILLISECONDS), Files.size(file)));
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new MojoFailureException("failed to get source failed", e);
		}

		final Map<String, String> existingClass = new HashMap<>();
		final Path local = Paths.get(src, packageName == null ? new String[0] : packageName.split("\\."));
		final int count = local.getNameCount() + 1;

		try { // Collect annotation
			Files.walkFileTree(Paths.get(src), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (!file.getFileName().toString().endsWith(".java"))
						return FileVisitResult.CONTINUE;
					parser.parse(file).ifSuccessful(descriptor);
					if (count == file.getNameCount() && file.startsWith(local)) {
						String string = file.getFileName().toString();
						string = string.substring(0, string.length() - 5);
						existingClass.put(string, packageName + "." + string);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new MojoFailureException("failed to get source failed", e);
		}
		SD d = descriptor.findServlet("/");
		if (d == null) {
			d = new SD(descriptor.servlets.size());
			d.clazz = DefaultServlet.class.getName();
			System.out.println(">> " + d.clazz);
			d.name = "default";
			d.pattern.add("/");
			descriptor.servlets.add(d);
		}
		getLog().info("descriptor:\n" + descriptor);

		// generate class
		types = new TypeCache(cu, existingClass);
		generateClass();

		try {
			new Output(output, packageName).save(cu);
		} catch (IOException e) {
			throw new MojoFailureException("failed to save output class", e);
		}
	}

	private void generateClass() {
		if (packageName != null && !packageName.isEmpty())
			cu.setPackageDeclaration(packageName);
		cl = cu.addClass(className, Modifier.Keyword.FINAL).addExtendedType(NIOServerCli.class).addImplementedType(HttpRawProcessor.class);
		cl.addFieldWithInitializer(types.get(Logger.class), "log", new MethodCallExpr(new TypeExpr(types.get(LoggerFactory.class)), "getLogger", Builder.list(new ClassExpr(types.get(cl)))), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

		cl.addFieldWithInitializer(types.get(int.class), "execMin", new IntegerLiteralExpr("0"))
				.setJavadocComment(new JavadocComment("min number of execution thread to use, default to 0"))
				.addAndGetAnnotation(Option.class)
				.addPair("names", new StringLiteralExpr("--exec-min"))
				.addPair("description", new StringLiteralExpr("min number of exec thread to use, default to 0"))
				.addPair("descriptionKey", new StringLiteralExpr("exec-min"));
		cl.addFieldWithInitializer(types.get(int.class), "execMax", new FieldAccessExpr(new TypeExpr(types.get(Integer.class)), "MAX_VALUE"))
				.setJavadocComment(new JavadocComment("max number of execution thread to use, default to Integer.MAX_VALUE"))
				.addAndGetAnnotation(Option.class)
				.addPair("names", new StringLiteralExpr("--exec-max"))
				.addPair("description", new StringLiteralExpr("max number of exec thread to use, default to Integer.MAX_VALUE"))
				.addPair("descriptionKey", new StringLiteralExpr("exec-max"));
		cl.addFieldWithInitializer(types.get(long.class), "execIdle", new IntegerLiteralExpr("60L"))
				.setJavadocComment(new JavadocComment("max idle time for exec thread, default to 60"))
				.addAndGetAnnotation(Option.class)
				.addPair("names", new StringLiteralExpr("--exec-idle"))
				.addPair("description", new StringLiteralExpr("max idle time for exec thread, default to 60"))
				.addPair("descriptionKey", new StringLiteralExpr("exec-idle"));
		cl.addFieldWithInitializer(types.get(int.class), "keepAliveIdle", new IntegerLiteralExpr("-1"))
				.setJavadocComment(new JavadocComment("max time to keep idle keepalive connection, default to -1"))
				.addAndGetAnnotation(Option.class)
				.addPair("names", new StringLiteralExpr("--keep-alive-idle"))
				.addPair("description", new StringLiteralExpr("max time to keep idle keepalive connection, default to -1"))
				.addPair("descriptionKey", new StringLiteralExpr("keep-alive-idle"));

		cl.addField(types.get(ServletManager.class), "SERVLETS", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
		cl.addField(types.get(EventManager.class), "EVENTS", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
		cl.addField(types.get(ServletContextImpl.class), "CTX", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

		for (Builder b : BUILDER)
			b.add(this);
	}

	@Override
	public ClassOrInterfaceDeclaration self() {
		return cl;
	}

	@Override
	public Descriptor descriptor() {
		return descriptor;
	}

	@Override
	public TypeCache type() {
		return types;
	}

	@Override
	public String sessionFactory() {
		return sessionFactory;
	}
}