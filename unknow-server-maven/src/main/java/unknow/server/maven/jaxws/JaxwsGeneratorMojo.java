/**
 * 
 */
package unknow.server.maven.jaxws;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import jakarta.jws.WebService;
import unknow.server.maven.AbstractGeneratorMojo;
import unknow.server.maven.TypeCache;
import unknow.server.maven.model_xml.XmlLoader;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxws-generator", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JaxwsGeneratorMojo extends AbstractGeneratorMojo {
	private static final Logger logger = LoggerFactory.getLogger(JaxwsGeneratorMojo.class);

	@org.apache.maven.plugins.annotations.Parameter(name = "publishUrl", defaultValue = "http://127.0.0.1:8080")
	private String publishUrl;
	@org.apache.maven.plugins.annotations.Parameter(name = "jaxbFactory")
	private String jaxbFactory;

	private final XmlLoader xmlLoader = new XmlLoader();

	@Override
	protected String id() {
		return "jaxws-generator";
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();
		processSrc();

		if (jaxbFactory == null)
			jaxbFactory = findJaxbFactory();
		if (jaxbFactory == null)
			throw new MojoFailureException("no jaxb factory found");

		for (TypeDeclaration<?> c : classes.values()) {
			Optional<AnnotationExpr> a = c.getAnnotationByClass(WebService.class);
			if (!a.isPresent())
				continue;
			CompilationUnit cu = newCu();
			TypeCache types = new TypeCache(cu, existingClass);

			try {
				new JaxwsServletBuilder(c.asClassOrInterfaceDeclaration(), loader, xmlLoader).generate(cu, types, publishUrl, jaxbFactory);
				out.save(cu);
			} catch (Exception e) {
				throw new MojoFailureException("failed to generate/save output class", e);
			}
		}
	}

	private String findJaxbFactory() {
		for (Resource r : project.getResources()) {
			Path p = Paths.get(r.getDirectory(), "META-INF/services/jakarta.xml.bind.JAXBContextFactory");
			if (!Files.exists(p))
				continue;
			try (BufferedReader i = Files.newBufferedReader(p)) {
				return i.readLine();
			} catch (Exception e) {
				logger.error("Failed to read service file", e);
			}
		}

		URL r = classLoader.getResource("META-INF/services/jakarta.xml.bind.JAXBContextFactory");
		if (r != null) {
			try (BufferedReader i = new BufferedReader(new InputStreamReader(r.openStream()))) {
				return i.readLine();
			} catch (Exception e) {
				logger.error("Failed to read service file", e);
			}
		}
		return null;
	}
}