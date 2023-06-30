/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;

public class OpenApiBuilder {
	private static final Logger logger = LoggerFactory.getLogger(OpenApiBuilder.class);
	public Info info;
	public List<Server> servers;
	public List<SecurityRequirement> security;
	public List<Tag> tags;
	public ExternalDocumentation externalDocs;

	public CompilationUnit build(MavenProject project, JaxrsModel model, String packageName, Map<String, String> existingClass) throws MojoExecutionException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectMapper m = new ObjectMapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.setDefaultPropertyInclusion(Include.NON_EMPTY);
		try {
			m.writeValue(bos, build(project, model));
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}

		CompilationUnit cu = new CompilationUnit(packageName);
		TypeCache types = new TypeCache(cu, existingClass);
		ClassOrInterfaceDeclaration cl = cu.addClass("OpenApi", Utils.PUBLIC).addSingleMemberAnnotation(WebServlet.class, Utils.text("/openapi.json"))
				.addExtendedType(types.getClass(HttpServlet.class));
		cl.addFieldWithInitializer(types.get(long.class), "serialVersionUID", new LongLiteralExpr("1"), Utils.PSF);
		cl.addFieldWithInitializer(types.get(byte[].class), "DATA", Utils.byteArray(bos.toByteArray()), Utils.PSF);

		cl.addMethod("doGet", Utils.PUBLIC).addMarkerAnnotation(Override.class).addThrownException(types.getClass(IOException.class))
				.addParameter(types.getClass(HttpServletRequest.class), "req").addParameter(types.getClass(HttpServletResponse.class), "res").getBody().get()
				.addStatement(new MethodCallExpr(new NameExpr("res"), "setContentType", Utils.list(Utils.text("application/json"))))
				.addStatement(new MethodCallExpr(new NameExpr("res"), "setContentLength", Utils.list(new IntegerLiteralExpr(Integer.toString(bos.size())))))
				.addStatement(new MethodCallExpr(new MethodCallExpr(new NameExpr("res"), "getOutputStream"), "write", Utils.list(new NameExpr("DATA"))));
		return cu;
	}

	private OpenAPI build(MavenProject project, JaxrsModel model) {
		OpenAPI spec = new OpenAPI();
		if (info == null)
			spec.setInfo(info = new Info());
		if (info.getTitle() == null)
			info.setTitle(project.getArtifactId());
		if (info.getVersion() == null)
			info.setVersion(project.getVersion());

		if (servers != null)
			spec.setServers(servers);
		if (security != null)
			spec.setSecurity(security);
		if (tags != null)
			spec.setTags(tags);
		if (externalDocs != null)
			spec.setExternalDocs(externalDocs);

		@SuppressWarnings("rawtypes")
		Map<String, Schema> schemas = new HashMap<>();
		io.swagger.v3.oas.models.Paths paths = new io.swagger.v3.oas.models.Paths();

		for (JaxrsMapping m : model.mappings()) {
			PathItem p = paths.computeIfAbsent(m.path, k -> new PathItem());

			Operation o = createOp(p, m.httpMethod);
			if (o == null) {
				logger.warn("can't map operation " + m.httpMethod + " " + m.path + " duplicate or unupported");
				continue;
			}

			// TODO get @Operation
//			o.set
//			PathItem pathItem = new PathItem();
//				model.mapping(p, m)
//			Operation operation = new Operation();

//			pathItem.setDelete(null);
		}
		// TODO add path
		// TODO add component/schema

		return spec.components(new Components().schemas(schemas)).paths(paths);
	}

	/**
	 * @param p
	 * @param httpMethod
	 * @return
	 */
	private static Operation createOp(PathItem p, String httpMethod) {
		Operation o = new Operation();
		switch (httpMethod) {
			case "GET":
				if (p.getGet() != null)
					return null;
				p.setGet(o);
				return o;
			case "PUT":
				if (p.getPut() != null)
					return null;
				p.setPut(o);
				return o;
			case "POST":
				if (p.getPost() != null)
					return null;
				p.setPost(o);
				return o;
			case "DELETE":
				if (p.getDelete() != null)
					return null;
				p.setDelete(o);
				return o;
			case "OPTIONS":
				if (p.getOptions() != null)
					return null;
				p.setOptions(o);
				return o;
			case "HEAD":
				if (p.getHead() != null)
					return null;
				p.setHead(o);
				return o;
			case "PATCH":
				if (p.getPatch() != null)
					return null;
				p.setPatch(o);
				return o;
			case "TRACE":
				if (p.getTrace() != null)
					return null;
				p.setTrace(o);
				return o;
			default:
				return null;
		}
	}
}