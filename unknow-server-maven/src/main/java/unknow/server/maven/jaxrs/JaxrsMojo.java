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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import unknow.server.maven.AbstractMojo;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
@Mojo(defaultPhase = LifecyclePhase.GENERATE_SOURCES, name = "jaxrs-generator", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class JaxrsMojo extends AbstractMojo {
	private static final Logger logger = LoggerFactory.getLogger(JaxrsMojo.class);

	private JaxrsModel model;

	private TypeCache types;
	private ClassOrInterfaceDeclaration cl;

	@Parameter(name = "openapi")
	private OpenApiBuilder openapi = new OpenApiBuilder();

	@Parameter(name = "basePath", defaultValue = "/")
	private String basePath;

	private BeanParamBuilder beans;
	private MediaTypesBuilder mt;

	@Override
	protected String id() {
		return "jaxrs-generator";
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		init();
		model = new JaxrsModel(loader, super.cl);
		processSrc(cu -> cu.walk(ClassOrInterfaceDeclaration.class, t -> model.process(loader.get(t.getFullyQualifiedName().get()).asClass())));
		model.implicitConstructor.remove("java.lang.String");

		beans = new BeanParamBuilder(newCu(), existingClass);
		mt = new MediaTypesBuilder(newCu(), existingClass);

		try {
			generateInitalizer();
		} catch (IOException e) {
			throw new MojoExecutionException(e);
		}

		if (basePath.endsWith("/"))
			basePath = basePath.substring(0, basePath.length() - 1);
		Map<String, List<JaxrsMapping>> map = new HashMap<>();
		for (JaxrsMapping m : model.mappings()) {
			String p = basePath + m.path;
			int i = p.indexOf('{');
			if (i > 0)
				p = p.substring(0, p.lastIndexOf('/', i)) + "/*";
			map.computeIfAbsent(p, k -> new ArrayList<>()).add(m);
		}

		for (Entry<String, List<JaxrsMapping>> e : map.entrySet())
			out.save(new JaxRsServletBuilder(newCu(), existingClass, e.getKey(), e.getValue(), beans, mt).build());

		out.save(openapi.build(project, model, basePath, newCu(), existingClass));
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
				.addParameter(types.getClass(Set.class, types.getClass(Class.class, TypeCache.ANY)), "c").addParameter(types.getClass(ServletContext.class), "ctx").getBody()
				.get().addStatement(new MethodCallExpr(new TypeExpr(types.getClass(RuntimeDelegate.class)), "setInstance",
						Utils.list(new ObjectCreationExpr(null, types.getClass(JaxrsRuntime.class), Utils.list()))));
		for (String s : model.converter)
			b.addStatement(new MethodCallExpr(ctx, "registerConverter", Utils.list(new ObjectCreationExpr(null, types.getClass(s), Utils.list()))));
		if (!model.implicitConstructor.isEmpty() || !model.implicitFromString.isEmpty() || !model.implicitValueOf.isEmpty())
			b.addStatement(new MethodCallExpr(ctx, "registerConverter", Utils.list(new ObjectCreationExpr(null, new ClassOrInterfaceType(null, "P"), Utils.list()))));

		for (Entry<ClassModel, List<String>> e : model.readers.entrySet()) {
			NodeList<Expression> l = new NodeList<>(new ObjectCreationExpr(null, types.getClass(e.getKey()), Utils.list()));
			l.add(e.getKey().annotation(Priority.class).flatMap(a -> a.value()).map(a -> (Expression) new IntegerLiteralExpr(a)).orElseGet(() -> new FieldAccessExpr(new TypeExpr(types.get(Priorities.class)), "USER")));
			for (String s : e.getValue())
				l.add(Utils.text(s));
			b.addStatement(new MethodCallExpr(ctx, "registerReader", l));
		}
		for (Entry<ClassModel, List<String>> e : model.writers.entrySet()) {
			NodeList<Expression> l = new NodeList<>(new ObjectCreationExpr(null, types.getClass(e.getKey()), Utils.list()));
			l.add(e.getKey().annotation(Priority.class).flatMap(a -> a.value()).map(a -> (Expression) new IntegerLiteralExpr(a)).orElseGet(() -> new FieldAccessExpr(new TypeExpr(types.get(Priorities.class)), "USER")));
			for (String s : e.getValue())
				l.add(Utils.text(s));
			b.addStatement(new MethodCallExpr(ctx, "registerWriter", l));
		}

		generateImplicitConverter(cu);

		for (Entry<TypeModel, ClassModel> e : model.exceptions.entrySet()) {

			b.addStatement(new MethodCallExpr(ctx, "registerException",
					Utils.list(new ClassExpr(types.get(e.getKey())), new ObjectCreationExpr(null, types.getClass(e.getValue()), Utils.list()))));
		}

		out.save(cu);

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
			e = new ObjectCreationExpr(null, type, Utils.list(new NameExpr("value")));
		else
			e = new MethodCallExpr(new TypeExpr(type), m, Utils.list(new NameExpr("value")));

		NodeList<BodyDeclaration<?>> methods = Utils.list(
				new MethodDeclaration(Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL), Utils.list(new MarkerAnnotationExpr("Override")),
						Utils.list(), type, new SimpleName("fromString"), Utils.list(new com.github.javaparser.ast.body.Parameter(types.getClass(String.class), "value")),
						Utils.list(), new BlockStmt().addStatement(new ReturnStmt(e))),
				new MethodDeclaration(Modifier.createModifierList(Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL), Utils.list(new MarkerAnnotationExpr("Override")),
						Utils.list(), types.getClass(String.class), new SimpleName("toString"), Utils.list(new com.github.javaparser.ast.body.Parameter(type, "value")),
						Utils.list(), new BlockStmt().addStatement(new ReturnStmt(new MethodCallExpr(new NameExpr("value"), "toString")))));
		clazz.addFieldWithInitializer(types.getClass(ParamConverter.class, type), name,
				new ObjectCreationExpr(null, types.getClass(ParamConverter.class, TypeCache.EMPTY), null, Utils.list(), methods), Utils.PSF);
	}

}
