/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam.JaxrsBeanFieldParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBodyParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsCookieParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsHeaderParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsMatrixParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsPathParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsQueryParam;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.EnumModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.TypeModel;

public class OpenApiBuilder {
	private static final Logger logger = LoggerFactory.getLogger(OpenApiBuilder.class);

	private static final String INTEGER = "integer";
	private static final String NUMBER = "number";
	private static final String STRING = "string";

	private static Schema<?> getDefault(String n) {
		switch (n) {
			case "boolean":
				return new Schema<>().type("boolean").nullable(false);
			case "java.lang.Boolean":
				return new Schema<>().type("boolean");

			case "byte":
				return new Schema<>().type(INTEGER).minimum(BigDecimal.valueOf(Byte.MIN_VALUE)).maximum(BigDecimal.valueOf(Byte.MAX_VALUE)).nullable(false);
			case "java.lang.Byte":
				return new Schema<>().type(INTEGER).minimum(BigDecimal.valueOf(Byte.MIN_VALUE)).maximum(BigDecimal.valueOf(Byte.MAX_VALUE));
			case "char":
				return new Schema<>().type(INTEGER).minimum(BigDecimal.valueOf(Character.MIN_VALUE)).maximum(BigDecimal.valueOf(Character.MAX_VALUE)).nullable(false);
			case "java.lang.Character":
				return new Schema<>().type(INTEGER).minimum(BigDecimal.valueOf(Character.MIN_VALUE)).maximum(BigDecimal.valueOf(Character.MAX_VALUE));
			case "short":
				return new Schema<>().type(INTEGER).minimum(BigDecimal.valueOf(Short.MIN_VALUE)).maximum(BigDecimal.valueOf(Short.MAX_VALUE)).nullable(false);
			case "java.lang.Short":
				return new Schema<>().type(INTEGER).minimum(BigDecimal.valueOf(Short.MIN_VALUE)).maximum(BigDecimal.valueOf(Short.MAX_VALUE));
			case "int":
				return new Schema<>().type(INTEGER).format("int32").nullable(false);
			case "java.lang.Integer":
				return new Schema<>().type(INTEGER).format("int32");
			case "long":
				return new Schema<>().type(INTEGER).format("int64").nullable(false);
			case "java.lang.Long":
				return new Schema<>().type(INTEGER).format("int64");
			case "java.math.BigInteger":
				return new Schema<>().type(INTEGER);

			case "float":
				return new Schema<>().type(NUMBER).format("float");
			case "java.lang.Float":
				return new Schema<>().type(NUMBER).format("float").nullable(false);
			case "double":
				return new Schema<>().type(NUMBER).format("double").nullable(false);
			case "java.lang.Double":
				return new Schema<>().type(NUMBER).format("double");
			case "java.math.BigDecimal":
				return new Schema<>().type(NUMBER);

			case "java.lang.String":
				return new Schema<>().type(STRING);

			case "java.time.LocalDate":
				return new Schema<>().type(STRING).format("date");
			case "java.time.LocalTime":
				return new Schema<>().type(STRING).format("time");
			case "java.time.LocalDateTime":
				return new Schema<>().type(STRING).format("date-time");
			case "java.time.ZonedDateTime":
				return new Schema<>().type(STRING).format("date-time");
			case "java.time.OffsetDateTime":
				return new Schema<>().type(STRING).format("date-time");
			case "java.time.Duration":
				return new Schema<>().type(STRING).format("duration");
			case "java.time.Period":
				return new Schema<>().type(STRING).format("period");
			default:
				return null;
		}
	}

	@SuppressWarnings("rawtypes")
	private final Map<String, Schema> schemas = new HashMap<>();

	public void build(OpenAPI spec, JaxrsModel model, String file) throws MojoExecutionException {

		ObjectMapper m = new ObjectMapper().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.setDefaultPropertyInclusion(Include.NON_EMPTY);
		try (OutputStream out = Files.newOutputStream(java.nio.file.Paths.get(file))) {
			m.writeValue(out, build(spec, model));
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}
	}

	private OpenAPI build(OpenAPI spec, JaxrsModel model) {

		io.swagger.v3.oas.models.Paths paths = new io.swagger.v3.oas.models.Paths();

		for (JaxrsMapping m : model.mappings()) {
			AnnotationModel a = m.m.annotation(io.swagger.v3.oas.annotations.Operation.class).orElse(null);
			if (a != null && a.member("hidden").map(v -> v.asBoolean()).orElse(false))
				continue;

			PathItem p = paths.computeIfAbsent(m.path, k -> new PathItem());

			Operation o = createOp(p, m.httpMethod);
			if (o == null) {
				logger.warn("can't map operation {} {} duplicate or unupported", m.httpMethod, m.path);
				continue;
			}
			AnnotationModel[] t = findTags(m.m);
			if (t != null) {
				List<String> tags = new ArrayList<>(t.length);
				for (int i = 0; i < t.length; i++)
					t[i].member("name").map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).ifPresent(v -> tags.add(v));
				o.setTags(tags);
			}

			ApiResponses responses = new ApiResponses();
			o.setResponses(responses);

			if (a != null) {
				a.member("tags").map(v -> v.asArrayLiteral()).filter(v -> v.length > 0).ifPresent(v -> o.setTags(Arrays.asList(v)));
				a.member("summary").map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).ifPresent(v -> o.setSummary(v));
				a.member("description").map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).ifPresent(v -> o.setDescription(v));
				a.member("operationId").map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).ifPresent(v -> o.setOperationId(v));
				a.member("deprecated").ifPresent(v -> o.setDeprecated(v.asBoolean()));

//				a.member("responses")

				// TODO externalDocs, responses, security, servers, extensions
			}

			if (responses.isEmpty() && !m.m.type().isVoid()) {
				Content c = new Content();
				for (String s : m.produce)
					c.addMediaType(s, new MediaType().schema(schema(m.m.type())));
				responses.setDefault(new ApiResponse().content(c));
			}

			for (JaxrsParam<?> param : m.params)
				addParameters(o, param);

		}
		spec.getComponents().setSchemas(schemas);
		return spec.paths(paths);
	}

	/**
	 * @param o
	 * @param param
	 */
	private void addParameters(Operation o, JaxrsParam<?> param) {
		if (param instanceof JaxrsBeanParam) {
			JaxrsBeanParam<?> b = (JaxrsBeanParam<?>) param;
			for (JaxrsBeanFieldParam p : b.params)
				addParameters(o, p.param);
			return;
		}

		if (param instanceof JaxrsBodyParam) {
			MediaType mdi = new MediaType();
			mdi.examples(null);
			mdi.schema(schema(param.type));
			Content content = new Content();
			content.addMediaType("*/*", mdi);
			o.setRequestBody(new RequestBody().content(content));
			return;
		}

		Parameter p = new Parameter().name(param.value).schema(schema(param.type));
		if (param instanceof JaxrsHeaderParam)
			p.setIn("header");
		else if (param instanceof JaxrsQueryParam)
			p.setIn("query");
		else if (param instanceof JaxrsPathParam)
			p.required(true).setIn("path");
		else if (param instanceof JaxrsCookieParam)
			p.setIn("cookie");
		else if (param instanceof JaxrsMatrixParam)
			p.in("path").style(StyleEnum.MATRIX);

		AnnotationModel a = param.p.annotation(io.swagger.v3.oas.annotations.Parameter.class).orElse(null);
		if (a != null) {
			if (a.member("hidden").map(v -> v.asBoolean()).orElse(false))
				return;

			a.member("name").map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).ifPresent(v -> p.setName(v));
			a.member("in").map(v -> v.asLiteral()).filter(v -> !"DEFAULT".equals(v)).ifPresent(v -> p.setIn(v));
			a.member("description").map(v -> v.asLiteral()).filter(v -> !v.isEmpty()).ifPresent(v -> p.setDescription(v));
			a.member("required").ifPresent(v -> p.setRequired(v.asBoolean()));
			a.member("deprecated").ifPresent(v -> p.setDeprecated(v.asBoolean()));
			// allowEmptyValue
			// style
			// explode
			// allowReserved
			// schema, array
			// content
			// examples, example
			// extensions
			// ref
		}

		o.addParametersItem(p);

	}

	/**
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Schema<?> schema(TypeModel type) {
		if (type.isWildCard())
			type = type.asWildcard().bound();

		String n = type.genericName();

		if (schemas.containsKey(n))
			return new Schema<>().$ref("#/components/schemas/" + n);

		Schema<?> s = getDefault(n);
		if (s != null)
			return s;

		if (type.isArray())
			return new Schema<>().type("array").items(schema(type.asArray().type()));

		if (type.isClass()) {
			ClassModel c = type.asClass().ancestor(Collection.class.getName());
			if (c != null)
				return new Schema<>().type("array").items(schema(c.parameter(0).type()));
			c = type.asClass().ancestor(Map.class.getName());
			if (c != null) {
				return new Schema<>().type("object").additionalProperties(schema(c.parameter(1).type()));
			}
		}
		schemas.put(n, s = new Schema<>());
		if (type.isEnum()) {
			EnumModel e = type.asEnum();
			// TODO get enum format/value from jackson
			// @JsonFormat on enum
			// @JsonValue on method
			// @JsonProperty on entries
			s.type(STRING).setEnum(e.entries().stream().map(v -> v.name()).collect(Collectors.toList()));
		} else {
			List<String> requied = new ArrayList<>();
			s.type("object").setRequired(requied);
			ClassModel c = type.asClass();

			for (FieldModel f : c.fields()) {
				s.addProperty(f.name(), schema(f.type()));
				// TODO add required value from jackson
			}
			// TODO exemple
		}
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

	public static AnnotationModel[] findTags(MethodModel m) {
		AnnotationModel[] t = m.annotation(Tags.class).flatMap(v -> v.value()).map(v -> v.asArrayAnnotation()).orElse(null);
		if (t == null)
			t = m.annotation(io.swagger.v3.oas.annotations.tags.Tag.class).map(v -> new AnnotationModel[] { v }).orElse(null);
		if (t != null)
			return t;
		return findTags(m.parent());
	}

	public static AnnotationModel[] findTags(ClassModel m) {
		if (m == null)
			return null;

		AnnotationModel[] t = m.annotation(Tags.class).flatMap(v -> v.value()).map(v -> v.asArrayAnnotation()).orElse(null);
		if (t == null)
			t = m.annotation(io.swagger.v3.oas.annotations.tags.Tag.class).map(v -> new AnnotationModel[] { v }).orElse(null);
		if (t == null)
			t = findTags(m.superType());
		Iterator<ClassModel> it = m.interfaces().iterator();
		while (t == null && it.hasNext())
			t = findTags(it.next());
		return t;
	}

	@SuppressWarnings("rawtypes")
	public Map<String, Schema> getSchemas() {
		return schemas;
	}
}