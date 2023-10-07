/**
 * 
 */
package unknow.server.maven.jaxws;

import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import jakarta.jws.WebService;
import unknow.server.maven.AbstractMojo;
import unknow.server.maven.TypeCache;
import unknow.server.maven.jaxb.model.XmlLoader;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxws-generator", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JaxwsGeneratorMojo extends AbstractMojo {

	@org.apache.maven.plugins.annotations.Parameter(name = "publishUrl", defaultValue = "http://127.0.0.1:8080")
	private String publishUrl;

	private final XmlLoader xmlLoader = new XmlLoader();

	@Override
	protected String id() {
		return "jaxws-generator";
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();
		processSrc();

		for (TypeDeclaration<?> c : classes.values()) {
			Optional<AnnotationExpr> a = c.getAnnotationByClass(WebService.class);
			if (!a.isPresent())
				continue;
			CompilationUnit cu = newCu();
			TypeCache types = new TypeCache(cu, existingClass);

			try {
				new JaxwsServletBuilder(c.asClassOrInterfaceDeclaration(), loader, xmlLoader).generate(cu, types, publishUrl);
//				out.save(cu);
			} catch (Exception e) {
				throw new MojoFailureException("failed to generate/save output class", e);
			}
		}
	}
}