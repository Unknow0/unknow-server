/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jws.WebService;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import unknow.server.maven.Output;
import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxws-generator")
public class JaxwsGenerator extends AbstractMojo {
	@org.apache.maven.plugins.annotations.Parameter(name = "src", defaultValue = "${project.build.sourceDirectory}")
	private String src;

	@org.apache.maven.plugins.annotations.Parameter(name = "packageName")
	private String packageName;

	@org.apache.maven.plugins.annotations.Parameter(name = "output")
	private String output;

	@org.apache.maven.plugins.annotations.Parameter(name = "publishUrl", defaultValue = "http://127.0.0.1:8080")
	private String publishUrl;

	private Output out;

	/** existing public class in output package (simpleName-&gt;fqn) */
	private final Map<String, String> existingClass = new HashMap<>();
	/** all class in src (fqn to classDef) */
	private final Map<String, TypeDeclaration<?>> classes = new HashMap<>();

	private JavaSymbolSolver javaSymbolSolver;
	private JavaParser parser;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		TypeSolver resolver = new CombinedTypeSolver(new JavaParserTypeSolver(src), new ReflectionTypeSolver(false));
		javaSymbolSolver = new JavaSymbolSolver(resolver);
		parser = new JavaParser(new ParserConfiguration().setStoreTokens(true).setSymbolResolver(javaSymbolSolver));

		if (output == null)
			output = src;

		try {
			out = new Output(output, packageName);
		} catch (IOException e) {
			throw new MojoFailureException("failed to create output folders", e);
		}

		final Path local = Paths.get(src, packageName == null ? new String[0] : packageName.split("\\."));
		final int count = local.getNameCount() + 1;

		try { // Collect existing classes
			Files.walkFileTree(Paths.get(src), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (!file.getFileName().toString().endsWith(".java"))
						return FileVisitResult.CONTINUE;
					parser.parse(file).ifSuccessful(t -> t.walk(TypeDeclaration.class, c -> classes.put(c.resolve().getQualifiedName(), c)));
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

		CompilationUnit marshallers = new CompilationUnit(packageName);
		marshallers.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);
		TypeCache types = new TypeCache(marshallers, existingClass);
		JaxMarshallerBuilder mbuilder = new JaxMarshallerBuilder(marshallers, types);
		boolean find = false;
		for (TypeDeclaration<?> c : classes.values()) {
			Optional<AnnotationExpr> a = c.getAnnotationByClass(WebService.class);
			if (!a.isPresent())
				continue;
			CompilationUnit cu = new CompilationUnit(packageName);
			cu.setData(Node.SYMBOL_RESOLVER_KEY, javaSymbolSolver);
			types = new TypeCache(cu, existingClass);

			new JaxwsServletBuilder(c.asClassOrInterfaceDeclaration(), classes, mbuilder).generate(cu, types, publishUrl);
			try {
				out.save(cu);
				find = true;
			} catch (IOException e) {
				throw new MojoFailureException("failed to save output class", e);
			}
		}
		if (find) {
			try {
				out.save(marshallers);
			} catch (IOException e) {
				throw new MojoFailureException("failed to save output class", e);
			}
		}
	}
}