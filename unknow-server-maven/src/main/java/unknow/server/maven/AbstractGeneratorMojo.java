/**
 * 
 */
package unknow.server.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;

import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.ast.AstModelLoader;
import unknow.server.maven.model.jvm.JvmModelLoader;

/**
 * @author unknow
 */
public abstract class AbstractGeneratorMojo extends AbstractMojo {
	private static final Logger logger = LoggerFactory.getLogger(AbstractGeneratorMojo.class);

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;

	@Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
	private RepositorySystemSession session;
	@org.apache.maven.plugins.annotations.Component
	protected RepositorySystem repository;

	@Parameter(name = "packageName")
	protected String packageName;

	@Parameter(name = "output")
	private String output;

	@Parameter(name = "resources")
	protected String resources;

	@Parameter(name = "graalvm", defaultValue = "true")
	protected boolean graalvm;

	@Parameter(name = "artifacts")
	protected List<String> artifacts;

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
	protected final Map<String, PackageDeclaration> packages = new HashMap<>();

	protected ModelLoader loader;

	protected ClassLoader classLoader;

	abstract protected String id();

	protected void init() throws MojoFailureException {
		classLoader = getClassLoader();
		loader = ModelLoader.from(JvmModelLoader.GLOBAL, new AstModelLoader(classes, packages), new JvmModelLoader(classLoader));

		List<String> compileSourceRoots = project.getCompileSourceRoots();

		List<TypeSolver> solver = new ArrayList<>(compileSourceRoots.size() + 1);
		solver.add(new ClassLoaderTypeSolver(classLoader));
		for (String s : compileSourceRoots) {
			if (Files.isDirectory(Paths.get(s)))
				solver.add(new JavaParserTypeSolver(s));
		}
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

	/**
	 * @return a newly created compilationUnit
	 */
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
			logger.error("Failed to get project classpath", e);
			return getClass().getClassLoader();
		}
	}

	protected void process(TypeConsumer c) throws MojoExecutionException, MojoFailureException {

		SrcWalker w = new SrcWalker();
		for (String s : project.getCompileSourceRoots())
			w.walk(s);
		for (String q : classes.keySet())
			c.accept(loader.get(q));

		if (artifacts == null || artifacts.isEmpty())
			return;
		try {
			for (String id : artifacts)
				parseArtifact(id, c);
		} catch (Exception e) {
			throw new MojoExecutionException(e);
		}
	}

	private void parseArtifact(String id, TypeConsumer c) throws ArtifactResolutionException, MojoExecutionException, MojoFailureException {
		ArtifactResult a = repository.resolveArtifact(session, new ArtifactRequest().setArtifact(new DefaultArtifact(id)));
		if (a == null)
			throw new MojoFailureException("Failed to resolve " + id);
		try (FileInputStream is = new FileInputStream(a.getArtifact().getFile()); ZipInputStream zip = new ZipInputStream(is)) {
			ZipEntry e;
			while ((e = zip.getNextEntry()) != null) {
				String name = e.getName();
				if (!name.endsWith(".class"))
					continue;
				c.accept(loader.get(name.substring(0, name.length() - 6).replaceAll("[/$]", ".")));
			}
		} catch (MojoExecutionException | MojoFailureException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoFailureException("Failed to resolve " + id, e);
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
		} catch (@SuppressWarnings("unused") IOException e) { // ignore
		}
	}

	private class SrcWalker extends SimpleFileVisitor<Path> {
		private final String[] part;
		private Path local;
		private int count;
		private Exception ex;

		public SrcWalker() {
			this.part = packageName == null ? new String[0] : packageName.split("\\.");
		}

		public void walk(String s) throws MojoFailureException, MojoExecutionException {
			Path path = Paths.get(s);
			if (!Files.isDirectory(path))
				return;

			local = Paths.get(s, part);
			count = local.getNameCount();
			ex = null;
			try {
				Files.walkFileTree(path, this);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to process source " + s, e);
			}
			if (ex instanceof MojoExecutionException)
				throw (MojoExecutionException) ex;
			if (ex instanceof MojoFailureException)
				throw (MojoFailureException) ex;
			if (ex != null)
				throw new MojoExecutionException("Failed to process source " + s, ex);
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			String f = file.getFileName().toString();
			if (!f.endsWith(".java"))
				return FileVisitResult.CONTINUE;
			ParseResult<CompilationUnit> parse = parser.parse(file);

			if (!parse.isSuccessful()) {
				logger.warn("Failed to parse {}: {}", f, parse.getProblems());
				return FileVisitResult.CONTINUE;
			}
			CompilationUnit cu = parse.getResult().orElse(null);
			if (cu == null)
				return FileVisitResult.CONTINUE;
			cu.getPackageDeclaration().filter(v -> v.getAnnotations() != null).ifPresent(v -> packages.put(v.getNameAsString(), v));
			for (TypeDeclaration<?> v : cu.findAll(TypeDeclaration.class)) {
				String qualifiedName = v.resolve().getQualifiedName();
				classes.put(qualifiedName, v);
			}
			if (count == file.getNameCount() && file.startsWith(local)) {
				String string = file.getFileName().toString();
				string = string.substring(0, string.length() - 5);
				existingClass.put(string, packageName + "." + string);
			}
			return FileVisitResult.CONTINUE;
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

	public static interface TypeConsumer {
		void accept(TypeModel t) throws MojoExecutionException, MojoFailureException;
	}
}
