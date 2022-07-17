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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.xml.sax.InputSource;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import unknow.sax.SaxParser;
import unknow.server.http.AbstractHttpServer;
import unknow.server.http.servlet.ServletResource;
import unknow.server.http.servlet.ServletResourceStatic;
import unknow.server.http.utils.Resource;
import unknow.server.maven.AbstractMojo;
import unknow.server.maven.TypeCache;
import unknow.server.maven.servlet.Builder.BuilderContext;
import unknow.server.maven.servlet.builder.CreateContext;
import unknow.server.maven.servlet.builder.CreateEventManager;
import unknow.server.maven.servlet.builder.CreateFilters;
import unknow.server.maven.servlet.builder.CreateServletManager;
import unknow.server.maven.servlet.builder.CreateServlets;
import unknow.server.maven.servlet.builder.Main;
import unknow.server.maven.servlet.descriptor.Descriptor;
import unknow.server.maven.servlet.descriptor.SD;
import unknow.server.maven.servlet.sax.Context;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "servlet-generator", requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ServletGenMojo extends AbstractMojo implements BuilderContext {
	private static final List<Builder> BUILDER = Arrays.asList(new CreateEventManager(), new CreateServletManager(), new CreateContext(), new CreateServlets(), new CreateFilters(), new Main());

	private final CompilationUnit cu = new CompilationUnit();

	private TypeCache types;

	private ClassOrInterfaceDeclaration cl;

	private final Descriptor descriptor = new Descriptor();

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(name = "className", defaultValue = "Server")
	private String className;

	@Parameter(name = "web-xml", defaultValue = "${project.basedir}/src/main/resources/WEB-INF/web.xml")
	private String webXml;

	@Parameter(name = "resources", defaultValue = "${project.basedir}/src/main/resources")
	private String resources;

	@Parameter(name = "staticResourceSize", defaultValue = "4096")
	private int staticResourceSize;

	@Parameter(name = "sessionFactory", defaultValue = "unknow.server.http.servlet.session.NoSessionFactory")
	private String sessionFactory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();
		cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);

		if (webXml != null) {
			Path path = Paths.get(webXml);
			if (Files.exists(path)) {
				try (InputStream r = Files.newInputStream(path)) {
					SaxParser.parse(new Context(descriptor, resolver), new InputSource(r));
				} catch (Exception e) {
					getLog().error("failed to parse '" + webXml + "'", e);
				}
			} else
				getLog().warn("missing '" + webXml + "'");
		}

		processSrc(descriptor);
		SD d = descriptor.findServlet("/");
		if (d == null)
			generateResources();

		getLog().info("descriptor:\n" + descriptor);

		// generate class
		types = new TypeCache(cu, existingClass);

		if (packageName != null && !packageName.isEmpty())
			cu.setPackageDeclaration(packageName);
		cl = cu.addClass(className, Modifier.Keyword.FINAL).addExtendedType(AbstractHttpServer.class);

		for (Builder b : BUILDER)
			b.add(this);

		try {
			out.save(cu);
		} catch (IOException e) {
			throw new MojoFailureException("failed to save output class", e);
		}
	}

	/**
	 * @throws MojoFailureException
	 * 
	 */
	private void generateResources() throws MojoFailureException {
		try { // collecting resources files
			Path path = Paths.get(resources);
			if (Files.isDirectory(path)) {
				Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						dir = path.relativize(dir);
						String path = dir.getName(0).toString().toUpperCase();
						return "WEB-INF".equals(path) || "META-INF".equals(path) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						String p = "/" + path.relativize(file).toString().replace('\\', '/');
						long size = Files.size(file);
						descriptor.resources.put(p, new Resource(Files.getLastModifiedTime(file).to(TimeUnit.MILLISECONDS), size));
						SD d = new SD(descriptor.servlets.size());
						d.clazz = (size < staticResourceSize ? ServletResourceStatic.class : ServletResource.class).getName();
						d.name = "Resource:" + p;
						d.pattern.add(p);
						descriptor.servlets.add(d);
						return FileVisitResult.CONTINUE;
					}
				});
			} else
				getLog().warn("Missing resource folder '" + resources + "'");
		} catch (IOException e) {
			throw new MojoFailureException("failed to get resources", e);
		}
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