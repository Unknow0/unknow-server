/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
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
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBodyParam;
import unknow.server.maven.model.TypeModel;

public class OpenApiBuilder {
	private static final Logger logger = LoggerFactory.getLogger(OpenApiBuilder.class);

	private static Schema<?> getDefault(String n) {
		switch (n) {
			case "boolean":
				return new Schema<>().type("boolean").nullable(false);
			case "java.lang.Boolean":
				return new Schema<>().type("boolean");

			case "byte":
				return new Schema<>().type("integer").minimum(BigDecimal.valueOf(Byte.MIN_VALUE)).maximum(BigDecimal.valueOf(Byte.MAX_VALUE)).nullable(false);
			case "java.lang.Byte":
				return new Schema<>().type("integer").minimum(BigDecimal.valueOf(Byte.MIN_VALUE)).maximum(BigDecimal.valueOf(Byte.MAX_VALUE));
			case "char":
				return new Schema<>().type("integer").minimum(BigDecimal.valueOf(Character.MIN_VALUE)).maximum(BigDecimal.valueOf(Character.MAX_VALUE)).nullable(false);
			case "java.lang.Character":
				return new Schema<>().type("integer").minimum(BigDecimal.valueOf(Character.MIN_VALUE)).maximum(BigDecimal.valueOf(Character.MAX_VALUE));
			case "short":
				return new Schema<>().type("integer").minimum(BigDecimal.valueOf(Short.MIN_VALUE)).maximum(BigDecimal.valueOf(Short.MAX_VALUE)).nullable(false);
			case "java.lang.Short":
				return new Schema<>().type("integer").minimum(BigDecimal.valueOf(Short.MIN_VALUE)).maximum(BigDecimal.valueOf(Short.MAX_VALUE));
			case "int":
				return new Schema<>().type("integer").format("int32").nullable(false);
			case "java.lang.Integer":
				return new Schema<>().type("integer").format("int32");
			case "long":
				return new Schema<>().type("integer").format("int64").nullable(false);
			case "java.lang.Long":
				return new Schema<>().type("integer").format("int64");
			case "java.math.BigInteger":
				return new Schema<>().type("integer");

			case "float":
				return new Schema<>().type("number").format("float");
			case "java.lang.Float":
				return new Schema<>().type("number").format("float").nullable(false);
			case "double":
				return new Schema<>().type("number").format("double").nullable(false);
			case "java.lang.Double":
				return new Schema<>().type("number").format("double");
			case "java.math.BigDecimal":
				return new Schema<>().type("number");

			case "java.lang.String":
				return new Schema<>().type("string");

			case "java.time.LocalDate":
				return new Schema<>().type("string").format("date");
			case "java.time.LocalTime":
				return new Schema<>().type("string").format("time");
			case "java.time.LocalDateTime":
				return new Schema<>().type("string").format("date-time");
			case "java.time.ZonedDateTime":
				return new Schema<>().type("string").format("date-time");
			case "java.time.OffsetDateTime":
				return new Schema<>().type("string").format("date-time");
			case "java.time.Duration":
				return new Schema<>().type("string").format("duration");
			case "java.time.Period":
				return new Schema<>().type("string").format("period");
			default:
				return null;
		}
	}

	public Info info;
	public List<Server> servers;
	public List<SecurityRequirement> security;
	public List<Tag> tags;
	public ExternalDocumentation externalDocs;

	@SuppressWarnings("rawtypes")
	private final Map<String, Schema> schemas = new HashMap<>();

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

		io.swagger.v3.oas.models.Paths paths = new io.swagger.v3.oas.models.Paths();

		for (JaxrsMapping m : model.mappings()) {
			PathItem p = paths.computeIfAbsent(m.path, k -> new PathItem());

			Operation o = createOp(p, m.httpMethod);
			if (o == null) {
				logger.warn("can't map operation " + m.httpMethod + " " + m.path + " duplicate or unupported");
				continue;
			}

			// TODO get @Operation
//			o.setOperationId(null);
//			o.setDescription(null);

			for (JaxrsParam param : m.params) {
				MediaType mdi = new MediaType();
				mdi.examples(null);
				mdi.schema(schema(param.type));
				Content content = new Content();
				content.addMediaType("*/*", mdi);
//				RequestBody r = new RequestBody();
//				r.description(null).setContent();
				if (param instanceof JaxrsBodyParam) {
					o.setRequestBody(new RequestBody().content(content));
				} else {
					String in = "";
					// header, query, path, cookie,
					o.addParametersItem(new Parameter().in(in).name(param.name).schema(schema(param.type)));

				}
			}

		}
		// TODO add path
		// TODO add component/schema

		return spec.components(new Components().schemas(schemas)).paths(paths);
	}

	/**
	 * @param type
	 * @return
	 */
	private Schema<?> schema(TypeModel type) {
		String n = type.name();

		// TODO array & collection & map
		// "array"
		if (schemas.containsKey(n))
			return new Schema<>().$ref("#/components/schemas/" + n);

		Schema<?> s = getDefault(n);
		if (s != null)
			return s;

		if (type.isEnum())
			s = new Schema<>()._enum(null);
		else {
			s = new Schema<>().type("object");
			// required => list of mendatory value
		}
		schemas.put(n, s);
		return new Schema<>().$ref("#/components/schemas/" + n);
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