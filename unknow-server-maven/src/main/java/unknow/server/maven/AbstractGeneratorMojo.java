/**
 * 
 */
package unknow.server.maven;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import unknow.server.maven.model.ModelLoader;

/**
 * @author unknow
 */
public abstract class AbstractGeneratorMojo extends org.apache.maven.plugin.AbstractMojo {
	@Parameter(name = "src", defaultValue = "${project.build.sourceDirectory}")
	protected String src;

	@Parameter(name = "packageName")
	protected String packageName;

	@Parameter(name = "output")
	private String output;

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

	protected final ModelLoader loader = new ModelLoader(classes);

	protected void init() throws MojoFailureException {
		resolver = new CombinedTypeSolver(new ReflectionTypeSolver(false), new JavaParserTypeSolver(src));
		javaSymbolSolver = new JavaSymbolSolver(resolver);
		parser = new JavaParser(new ParserConfiguration().setStoreTokens(true).setSymbolResolver(javaSymbolSolver));

		if (output == null)
			output = src;

		try {
			out = new Output(output, packageName);
		} catch (IOException e) {
			throw new MojoFailureException("failed to create output folders", e);
		}
	}

	protected void processSrc(Consumer<CompilationUnit> consumer) throws MojoFailureException {
		final Path local = Paths.get(src, packageName == null ? new String[0] : packageName.split("\\."));
		final int count = local.getNameCount() + 1;

		Consumer<CompilationUnit> p = consumer == null ? c : cu -> {
			c.accept(cu);
			consumer.accept(cu);
		};

		try { // Collect annotation
			Files.walkFileTree(Paths.get(src), new SimpleFileVisitor<Path>() {
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
		} catch (IOException e) {
			throw new MojoFailureException("failed to get source failed", e);
		}
	}
}
