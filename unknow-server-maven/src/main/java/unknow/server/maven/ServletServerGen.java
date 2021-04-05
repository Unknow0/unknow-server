package unknow.server.maven;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.printer.PrettyPrinterConfiguration;
import com.github.javaparser.printer.PrettyPrinterConfiguration.IndentType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import unknow.server.http.HttpRawProcessor;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.ServletManager;
import unknow.server.maven.builder.Call;
import unknow.server.maven.builder.Constructor;
import unknow.server.maven.builder.CreateContext;
import unknow.server.maven.builder.CreateEventManager;
import unknow.server.maven.builder.CreateServletManager;
import unknow.server.maven.builder.Initialize;
import unknow.server.maven.builder.LoadInitializer;
import unknow.server.maven.builder.Main;
import unknow.server.maven.builder.Process;
import unknow.server.maven.descriptor.Descriptor;
import unknow.server.maven.sax.HandlerContext;
import unknow.server.maven.sax.WebApp;
import unknow.server.nio.cli.NIOServerCli;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "servlet-generator")
public class ServletServerGen extends AbstractMojo {
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
	private String packageName = "unknow.server.http.test";

	@Parameter(name = "output")
	private String output;

	@Parameter(name = "web-xml")
	private String webXml;

	@Parameter(name = "charset", defaultValue = "UTF8")
	private Charset charset;

	public void setCharset(String charset) {
		this.charset = Charset.forName(charset);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(new CombinedTypeSolver(new ReflectionTypeSolver(), new JavaParserTypeSolver(src)));
		parser = new JavaParser(new ParserConfiguration().setStoreTokens(true).setSymbolResolver(javaSymbolSolver));
		cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);

		if (output == null)
			output = src;

		if (webXml != null) {
			try {
				SAXParserFactory newInstance = SAXParserFactory.newInstance();
				newInstance.setNamespaceAware(true);
				SAXParser parser = newInstance.newSAXParser();
				XMLReader xmlReader = parser.getXMLReader();
				xmlReader.setContentHandler(new WebApp(new HandlerContext(new StringBuilder(), descriptor, xmlReader)));
				xmlReader.setErrorHandler(new ErrorHandler() {
					@Override
					public void warning(SAXParseException exception) throws SAXException {
						getLog().warn("failed to parse '" + webXml + "'", exception);
					}

					@Override
					public void fatalError(SAXParseException exception) throws SAXException {
						getLog().error("failed to parse '" + webXml + "'", exception);
					}

					@Override
					public void error(SAXParseException exception) throws SAXException {
						getLog().error("failed to parse '" + webXml + "'", exception);
					}
				});
				try (InputStream r = Files.newInputStream(Paths.get(webXml))) {
					xmlReader.parse(new InputSource(r));
				}
			} catch (Exception e) {
				getLog().error("failed to parse '" + webXml + "'", e);
			}

			System.out.println(">> " + descriptor);
		}

		final Set<String> existingClass = new HashSet<>();
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
						existingClass.add(string.substring(0, string.length() - 5));
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new MojoFailureException("failed to get source failed", e);
		}
		// TODO read web.xml

		// generate class
		types = new TypeCache(cu, existingClass);
		generateClass();
	}

	private void generateClass() throws MojoFailureException {
		if (packageName != null && !packageName.isEmpty())
			cu.setPackageDeclaration(packageName);
		cl = cu.addClass(className, Modifier.Keyword.FINAL).addExtendedType(NIOServerCli.class).addImplementedType(HttpRawProcessor.class);
		cl.addFieldWithInitializer(types.get(Logger.class), "log", new MethodCallExpr(new TypeExpr(types.get(LoggerFactory.class)), "getLogger", Builder.list(new ClassExpr(types.get(cl)))), Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

		cl.addField(types.get(ServletManager.class), "SERVLETS", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
		cl.addField(types.get(EventManager.class), "EVENTS", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);
		cl.addField(types.get(ServletContextImpl.class), "CTX", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

		for (Builder b : BUILDER)
			b.add(cl, descriptor, types);

		PrettyPrinterConfiguration pp = new PrettyPrinterConfiguration();
		pp.setIndentType(IndentType.TABS).setIndentSize(1);
		pp.setOrderImports(true).setSpaceAroundOperators(true);

		Path file = Paths.get(output);
		if (packageName != null)
			file = file.resolve(packageName.replace('.', '/'));

		try (BufferedWriter w = Files.newBufferedWriter(file.resolve(className + ".java"), charset)) {
			w.write(cu.toString(pp));
		} catch (IOException e) {
			throw new MojoFailureException("failed to write output class", e);
		}
	}
}