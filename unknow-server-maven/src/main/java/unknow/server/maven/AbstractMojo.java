/**
 * 
 */
package unknow.server.maven;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;

import unknow.server.maven.model.ModelLoader;

/**
 * @author unknow
 */
public abstract class AbstractMojo extends org.apache.maven.plugin.AbstractMojo {
	private static final Logger log = LoggerFactory.getLogger(AbstractMojo.class);

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;

	@Parameter(name = "packageName")
	protected String packageName;

	@Parameter(name = "output")
	private String output;

	@Parameter(name = "resources")
	protected String resources;

	protected Output out;

	/** created with init() */
	protected JavaParser parser;
	/** created with init() */
	protected TypeSolver resolver;
	/** created with init() */
	protected JavaSymbolSolver javaSymbolSolver;

	/**
	 * existing public class in output package (simpleName-&gt;fqn).<br>
	 * populated with processSrc
	 */
	protected final Map<String, String> existingClass = new HashMap<>();

	/** all class in src (fqn to classDef) */
	protected final Map<String, TypeDeclaration<?>> classes = new HashMap<>();
	private final Consumer<CompilationUnit> c = cu -> cu.walk(TypeDeclaration.class, c -> classes.put(c.resolve().getQualifiedName(), c));

	protected ModelLoader loader;

	abstract protected String id();

	protected void init() throws MojoFailureException {
		ClassLoader cl = getClassLoader();
		loader = new ModelLoader(cl, classes);

		List<String> compileSourceRoots = project.getCompileSourceRoots();

		TypeSolver[] solver = new TypeSolver[compileSourceRoots.size() + 1];
		int i = 0;
		solver[i++] = new ClassLoaderTypeSolver(cl);
		for (String s : compileSourceRoots)
			solver[i++] = new JavaParserTypeSolver(s);
		resolver = new CombinedTypeSolver(solver);
		javaSymbolSolver = new JavaSymbolSolver(resolver);
		parser = new JavaParser(new ParserConfiguration().setStoreTokens(true).setSymbolResolver(javaSymbolSolver));

		if (output == null)
			output = project.getBuild().getDirectory() + "/" + id() + "/src";
		project.addCompileSourceRoot(output);

		if (resources == null)
			resources = project.getBuild().getDirectory() + "/" + id() + "/resources";
		addResource(resources);

		try {
			out = new Output(output, packageName);
		} catch (IOException e) {
			throw new MojoFailureException("failed to create output folders", e);
		}
	}

	public CompilationUnit newCu() {
		CompilationUnit cu = new CompilationUnit(packageName);
		cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);
		return cu;
	}

	private ClassLoader getClassLoader() {
		try {
			List<String> classpathElements = project.getRuntimeClasspathElements();
			URL urls[] = new URL[classpathElements.size()];

			for (int i = 0; i < urls.length; i++)
				urls[i] = new File(classpathElements.get(i)).toURI().toURL();

			return new URLClassLoader(urls, getClass().getClassLoader());
		} catch (Exception e) {
			log.error("Failed to get project classpath", e);
			return getClass().getClassLoader();
		}
	}

	protected void processSrc() throws MojoFailureException {
		processSrc(null);
	}

	protected void processSrc(Consumer<CompilationUnit> consumer) throws MojoFailureException {
		String[] part = packageName == null ? new String[0] : packageName.split("\\.");

		Consumer<CompilationUnit> p = consumer == null ? c : cu -> {
			c.accept(cu);
			consumer.accept(cu);
		};

		try { // Collect annotation
			for (String s : project.getCompileSourceRoots()) {
				final Path local = Paths.get(s, part);
				final int count = local.getNameCount();

				Files.walkFileTree(Paths.get(s), new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (!file.getFileName().toString().endsWith(".java"))
							return FileVisitResult.CONTINUE;
						parser.parse(file).ifSuccessful(p);
						if (count == file.getNameCount() && file.startsWith(local)) {
							String string = file.getFileName().toString();
							string = string.substring(0, string.length() - 5);
							existingClass.put(string, packageName + "." + string);
						}
						return FileVisitResult.CONTINUE;
					}
				});
			}
		} catch (IOException e) {
			throw new MojoFailureException("failed to get source failed", e);
		}
	}

	protected void processResources(BiConsumer<Path, Path> c) {
		DirectoryWalker scanner = new DirectoryWalker();
		L l = new L(c);
		scanner.addDirectoryWalkListener(l);
		for (Resource r : project.getResources()) {
			l.root = Paths.get(r.getDirectory());
			if (!Files.exists(l.root))
				continue;
			scanner.setBaseDir(l.root.toFile());
			scanner.setIncludes(r.getIncludes());
			scanner.setExcludes(r.getExcludes());
			scanner.scan();
		}
	}

	private void addResource(String resources) {
		for (Resource e : project.getResources()) {
			if (resources.equals(e.getDirectory()))
				return;
		}
		Resource resource = new Resource();
		resource.setDirectory(resources);
		project.addResource(resource);

		try {
			Files.createDirectories(Paths.get(resources));
		} catch (IOException e) { // ignore
		}
	}

	private static class L implements DirectoryWalkListener {
		final BiConsumer<Path, Path> c;
		Path root;

		L(BiConsumer<Path, Path> c) {
			this.c = c;
		}

		@Override
		public void directoryWalkStep(int percentage, File file) {
			Path path = file.toPath();
			c.accept(path, root.relativize(path));
		}

		@Override
		public void directoryWalkStarting(File basedir) { // ok
		}

		@Override
		public void directoryWalkFinished() { // ok
		}

		@Override
		public void debug(String message) { // ok
		}
	}
}
