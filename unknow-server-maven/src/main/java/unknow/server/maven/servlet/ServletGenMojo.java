package unknow.server.maven.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Iterator;
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

import jakarta.servlet.DispatcherType;
import unknow.sax.SaxParser;
import unknow.server.http.AbstractHttpServer;
import unknow.server.http.AccessLogFilter;
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

	private static final SD ACCESSLOG = new SD(0);
	static {
		ACCESSLOG.clazz = AccessLogFilter.class.getName();
		ACCESSLOG.dispatcher.add(DispatcherType.REQUEST);
		ACCESSLOG.pattern.add("/*");
		ACCESSLOG.name = "acessLog";
	}

	private final CompilationUnit cu = new CompilationUnit();

	private TypeCache types;

	private ClassOrInterfaceDeclaration cl;

	private final Descriptor descriptor = new Descriptor();

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = "Server")
	private String className;

	@Parameter(defaultValue = "${project.basedir}/src/main/resources/WEB-INF/web.xml")
	private String webXml;

	@Parameter(defaultValue = "${project.basedir}/src/main/resources")
	private String resources;

	@Parameter(defaultValue = "4096")
	private int staticResourceSize;

	@Parameter(defaultValue = "unknow.server.http.servlet.session.NoSessionFactory")
	private String sessionFactory;

	@Parameter(defaultValue = "true")
	private boolean graalvm;

	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/native-image/servletgen/resource-config.json")
	private String graalvmResouceConfig;

	@Parameter(defaultValue = "true")
	private boolean addAccessLog;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();

		cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);
		if (addAccessLog)
			descriptor.filters.add(ACCESSLOG);

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
		generateResources();
		if (graalvm && !descriptor.resources.isEmpty())
			generateGraalvmResources();

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
	private void generateGraalvmResources() throws MojoFailureException {
		try {
			Path path = Paths.get(graalvmResouceConfig);
			Files.createDirectories(path.getParent());
			try (BufferedWriter w = Files.newBufferedWriter(path)) {
				w.write("{\"resources\":{\"includes\":[");
				Iterator<String> it = descriptor.resources.keySet().iterator();
				w.append("{\"pattern\":\"").append(it.next().substring(1)).write("\"}");
				while (it.hasNext())
					w.append(",{\"pattern\":\"").append(it.next().substring(1)).write("\"}");
				w.write("]}}");
			}
		} catch (IOException e) {
			throw new MojoFailureException("failed generate graalvm resources", e);
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