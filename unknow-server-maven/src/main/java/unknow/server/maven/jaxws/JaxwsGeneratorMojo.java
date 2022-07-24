/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.IOException;
import java.util.Optional;

import javax.jws.WebService;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import unknow.server.maven.AbstractMojo;
import unknow.server.maven.TypeCache;
import unknow.server.maven.jaxws.binding.XmlTypeLoader;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxws-generator")
public class JaxwsGeneratorMojo extends AbstractMojo {

	@org.apache.maven.plugins.annotations.Parameter(name = "publishUrl", defaultValue = "http://127.0.0.1:8080")
	private String publishUrl;

	private final XmlTypeLoader xmlLoader = new XmlTypeLoader();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();
		processSrc(null);

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

			try {
				new JaxwsServletBuilder(c.asClassOrInterfaceDeclaration(), loader, mbuilder, xmlLoader).generate(cu, types, publishUrl);
				out.save(cu);
				find = true;
			} catch (Exception e) {
				throw new MojoFailureException("failed to generate/save output class", e);
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