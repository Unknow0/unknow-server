/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;

import jakarta.annotation.Priority;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.RuntimeDelegate;
import unknow.server.http.jaxrs.JaxrsContext;
import unknow.server.http.jaxrs.JaxrsRuntime;
import unknow.server.http.jaxrs.protostuff.ProtostuffSchema;
import unknow.server.maven.AbstractGeneratorMojo;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxrs-generator", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JaxrsMojo extends AbstractGeneratorMojo {
	private static final String VALUE = "value";

	private JaxrsModel model;

	private TypeCache types;
	private ClassOrInterfaceDeclaration cl;

	@Parameter(name = "openapi")
	private OpenApiConfig openapi = new OpenApiConfig();

	@Parameter(name = "basePath", defaultValue = "/")
	private String basePath;

	@Parameter(name = "objectMapper")
	private String objectMapper;

	private BeanParamBuilder beans;
	private MediaTypesBuilder mt;

	@Override
	protected String id() {
		return "jaxrs-generator";
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();
		if (basePath.endsWith("/"))
			basePath = basePath.substring(0, basePath.length() - 1);
		model = new JaxrsModel(loader, classLoader, basePath);

		process(t -> t.ifClass(model::process));
		model.implicitConstructor.remove("java.lang.String");

		beans = new BeanParamBuilder(newCu(), existingClass);
		mt = new MediaTypesBuilder(newCu(), existingClass);

		try {
			generateInitalizer();
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}

		Map<String, List<JaxrsMapping>> map = new HashMap<>();
		for (JaxrsMapping m : model.mappings()) {
			String p = m.path;
			int i = p.indexOf('{');
			if (i > 0)
				p = p.substring(0, p.lastIndexOf('/', i)) + "/*";
			map.computeIfAbsent(p, k -> new ArrayList<>()).add(m);
		}

		for (Entry<String, List<JaxrsMapping>> e : map.entrySet())
			out.save(new JaxRsServletBuilder(newCu(), existingClass, e.getKey(), e.getValue(), beans, mt).build());

		new OpenApiBuilder().build(openapi.getSpec(project), model, resources + basePath + "/openapi.json");
		beans.save(out);
		mt.save(out);
	}

	/**
	 * 
	 */

	private void generateInitalizer() throws IOException, MojoExecutionException {
		Path path = Paths.get(resources, "META-INF", "services", ServletContainerInitializer.class.getName());
		Files.createDirectories(path.getParent());
		try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			w.append(packageName).write(".JaxrsInit\n");
		}

		CompilationUnit cu = newCu();
		types = new TypeCache(cu, existingClass);

		TypeExpr ctx = new TypeExpr(types.getClass(JaxrsContext.class));

		cl = cu.addClass("JaxrsInit", Utils.PUBLIC).addImplementedType(ServletContainerInitializer.class);
		BlockStmt b = cl.addMethod("onStartup", Utils.PUBLIC).addMarkerAnnotation(Override.class)
				.addParameter(types.getClass(Set.class, types.getClass(Class.class, TypeCache.ANY)), "c").addParameter(types.getClass(ServletContext.class), "ctx")
				.createBody().addStatement(new MethodCallExpr(new TypeExpr(types.getClass(RuntimeDelegate.class)), "setInstance",
						Utils.list(new ObjectCreationExpr(null, types.getClass(JaxrsRuntime.class), Utils.list()))));
		for (ClassModel s : model.converter) {
			ClassOrInterfaceType t = types.getClass(s);
			if (!s.parameters().isEmpty()) {
				t = t.clone();
				t.setTypeArguments(Utils.list(TypeCache.EMPTY));
			}
			b.addStatement(new MethodCallExpr(ctx, "registerConverter", Utils.list(new ObjectCreationExpr(null, t, Utils.list()))));
		}
		if (!model.implicitConstructor.isEmpty() || !model.implicitFromString.isEmpty() || !model.implicitValueOf.isEmpty())
			b.addStatement(new MethodCallExpr(ctx, "registerConverter", Utils.list(new ObjectCreationExpr(null, new ClassOrInterfaceType(null, "P"), Utils.list()))));

		Expression m = null;
		if (objectMapper != null) {
			ParseResult<Expression> p = parser.parseExpression(objectMapper);
			if (p.isSuccessful())
				m = p.getResult().orElse(null);
		}

		for (Entry<ClassModel, List<String>> e : model.readers.entrySet())
			b.addStatement(new MethodCallExpr(ctx, "registerReader", registerParams(e.getKey(), e.getValue(), m)));
		for (Entry<ClassModel, List<String>> e : model.writers.entrySet())
			b.addStatement(new MethodCallExpr(ctx, "registerWriter", registerParams(e.getKey(), e.getValue(), m)));
		generateImplicitConverter(cu);

		for (Entry<TypeModel, ClassModel> e : model.exceptions.entrySet()) {

			b.addStatement(new MethodCallExpr(ctx, "registerException",
					Utils.list(new ClassExpr(types.get(e.getKey())), new ObjectCreationExpr(null, types.getClass(e.getValue()), Utils.list()))));
		}

		for (String clazz : model.protostuffMessage) {
			ClassOrInterfaceType type = types.getClass(clazz);
			b.addStatement(new MethodCallExpr(new TypeExpr(types.get(ProtostuffSchema.class)), "register",
					Utils.list(new ClassExpr(type), new MethodCallExpr(new ObjectCreationExpr(null, type, Utils.list()), "cachedSchema"))));
		}

		out.save(cu);
	}

	private NodeList<Expression> registerParams(ClassModel c, List<String> mimes, Expression m) {
		NodeList<Expression> list = Utils.list();
		if (m != null && c.constructors(loader.get(ObjectMapper.class.getName())).isPresent())
			list.add(m);
		ClassOrInterfaceType t = types.getClass(c);
		if (!c.parameters().isEmpty()) {
			t = t.clone();
			t.setTypeArguments(Utils.list(TypeCache.EMPTY));
		}
		NodeList<Expression> l = new NodeList<>(new ObjectCreationExpr(null, t, list));
		l.add(c.annotation(Priority.class).flatMap(a -> a.value()).filter(v -> v.isSet()).map(a -> (Expression) new IntegerLiteralExpr(a.asLiteral()))
				.orElseGet(() -> new FieldAccessExpr(new TypeExpr(types.get(Priorities.class)), "USER")));
		for (String s : mimes)
			l.add(Utils.text(s));
		return l;
	}

	private void generateImplicitConverter(CompilationUnit cu) {
		if (model.implicitConstructor.isEmpty() && model.implicitFromString.isEmpty() && model.implicitValueOf.isEmpty())
			return;
		TypeParameter t = new TypeParameter("T");
		ClassOrInterfaceType p = types.getClass(ParamConverter.class, t);
		ClassOrInterfaceDeclaration clazz = cu.addClass("P", Modifier.Keyword.FINAL).addImplementedType(ParamConverterProvider.class);
		BlockStmt b = new BlockStmt();

		int i = 0;
		for (String c : model.implicitConstructor) {
			String n = "c$" + i++;
			generateImplicitConverter(clazz, n, c, null);
			b.addStatement(new IfStmt(new BinaryExpr(new ClassExpr(types.getClass(c)), new NameExpr("rawType"), BinaryExpr.Operator.EQUALS),
					new ReturnStmt(new CastExpr(p, new NameExpr(n))), null));
		}
		for (String c : model.implicitFromString) {
			String n = "c$" + i++;
			generateImplicitConverter(clazz, n, c, "fromString");
			b.addStatement(new IfStmt(new BinaryExpr(new ClassExpr(types.getClass(c)), new NameExpr("rawType"), BinaryExpr.Operator.EQUALS),
					new ReturnStmt(new CastExpr(p, new NameExpr(n))), null));
		}
		for (String c : model.implicitValueOf) {
			String n = "c$" + i++;
			generateImplicitConverter(clazz, n, c, "valueOf");
			b.addStatement(new IfStmt(new BinaryExpr(new ClassExpr(types.getClass(c)), new NameExpr("rawType"), BinaryExpr.Operator.EQUALS),
					new ReturnStmt(new CastExpr(p, new NameExpr(n))), null));
		}

		clazz.addMethod("getConverter", Utils.PUBLIC).addMarkerAnnotation(Override.class).addSingleMemberAnnotation(SuppressWarnings.class, Utils.text("unchecked"))
				.addTypeParameter(t).setType(p).addParameter(types.getClass(Class.class, t), "rawType").addParameter(types.getClass(Type.class), "genericType")
				.addParameter(types.array(Annotation.class), "Annotation").setBody(b.addStatement(new ReturnStmt(new NullLiteralExpr())));
	}

	private void generateImplicitConverter(ClassOrInterfaceDeclaration clazz, String name, String cl, String m) {
		ClassOrInterfaceType type = types.getClass(cl);
		Expression e;
		if (m == null)
			e = new ObjectCreationExpr(null, type, Utils.list(new NameExpr(VALUE)));
		else
			e = new MethodCallExpr(new TypeExpr(type), m, Utils.list(new NameExpr(VALUE)));

		NodeList<BodyDeclaration<?>> methods = Utils.list(
				new MethodDeclaration(Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL), Utils.list(new MarkerAnnotationExpr("Override")),
						Utils.list(), type, new SimpleName("fromString"), Utils.list(new com.github.javaparser.ast.body.Parameter(types.getClass(String.class), VALUE)),
						Utils.list(), new BlockStmt().addStatement(new ReturnStmt(e))),
				new MethodDeclaration(Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL), Utils.list(new MarkerAnnotationExpr("Override")),
						Utils.list(), types.getClass(String.class), new SimpleName("toString"), Utils.list(new com.github.javaparser.ast.body.Parameter(type, VALUE)),
						Utils.list(), new BlockStmt().addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr(VALUE), "toString")))));
		clazz.addFieldWithInitializer(types.getClass(ParamConverter.class, type), name,
				new ObjectCreationExpr(null, types.getClass(ParamConverter.class, TypeCache.EMPTY), null, Utils.list(), methods), Utils.PSF);
	}

}
