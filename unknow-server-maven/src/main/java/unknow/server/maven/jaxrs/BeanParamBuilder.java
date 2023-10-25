/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.TryStmt;

import jakarta.ws.rs.ext.ParamConverter;
import unknow.server.http.jaxrs.JaxrsContext;
import unknow.server.http.jaxrs.JaxrsEntityReader;
import unknow.server.http.jaxrs.JaxrsReq;
import unknow.server.maven.Output;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBeanParam.JaxrsBeanFieldParam;
import unknow.server.maven.jaxrs.JaxrsParam.JaxrsBodyParam;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class BeanParamBuilder {
	private final CompilationUnit cu;
	private final ClassOrInterfaceDeclaration cl;
	private final TypeCache types;

	private final Map<String, JaxrsBeanParam<?>> beans;
	private final Map<String, String> beansVar;

	private final Map<JaxrsParam<?>, String> converterVar;

	public BeanParamBuilder(CompilationUnit cu, Map<String, String> existingClass) {
		this.cu = cu;
		types = new TypeCache(cu, existingClass);
		cl = cu.addClass("BeansReader", Utils.PUBLIC);

		beans = new HashMap<>();
		beansVar = new HashMap<>();
		converterVar = new HashMap<>();
	}

	public String get(JaxrsBeanParam<?> param) {
		return beansVar.get(param.clazz.name());
	}

	public void add(JaxrsBeanParam<?> param) {
		String clazz = param.clazz.name();
		if (beans.containsKey(clazz))
			return;
		BlockStmt init = cl.addStaticInitializer().addStatement(new VariableDeclarationExpr(types.getClass(Type.class), "t"))
				.addStatement(new VariableDeclarationExpr(types.array(Annotation.class), "a"));

		String n = "$" + beansVar.size();
		beans.put(clazz, param);
		beansVar.put(clazz, "build" + param.clazz.simpleName() + n);
		int i = 0;
		for (JaxrsBeanFieldParam p : param.params)
			processBeanConverter(p.param, n + "$" + i++, init);

		BlockStmt b = cl.addMethod(beansVar.get(clazz), Utils.PUBLIC_STATIC).addParameter(types.getClass(JaxrsReq.class), "r").setType(types.get(clazz)).createBody()
				.addStatement(Utils.create(types.getClass(clazz), "b", Utils.list()));
		for (JaxrsBeanFieldParam e : param.params) {
			if (e.setter != null)
				b.addStatement(new MethodCallExpr(new NameExpr("b"), e.setter.name(), Utils.list(getParam(e.param))));
			else
				b.addStatement(new AssignExpr(new FieldAccessExpr(new NameExpr("b"), e.field.name()), getParam(e.param), AssignExpr.Operator.ASSIGN));

		}
		b.addStatement(new ReturnStmt(new NameExpr("b")));
	}

	public void save(Output out) throws MojoExecutionException {
		if (!beans.isEmpty())
			out.save(cu);
	}

	private void processBeanConverter(JaxrsParam<?> p, String n, BlockStmt b) {
		if (p instanceof JaxrsBeanParam) {
			add((JaxrsBeanParam<?>) p);
			return;
		}
		converterVar.put(p, n);

		TypeModel t = JaxrsModel.getParamType(p.type);
		TypeModel t1 = t;
		if (t.isWildCard()) {
			t1 = t.asWildcard().bound();
			if (t1 == null)
				t1 = ModelLoader.OBJECT;
		}

		b.addStatement(
				new TryStmt(
						new BlockStmt()
								.addStatement(Utils.assign(types.getClass(Field.class), "f",
										new MethodCallExpr(new ClassExpr(types.get(p.parent.name())), "getDeclaredField", Utils.list(Utils.text(p.name)))))
								.addStatement(new AssignExpr(new NameExpr("t"),
										new MethodCallExpr(new TypeExpr(types.get(JaxrsContext.class)), "getParamType",
												Utils.list(new MethodCallExpr(new NameExpr("f"), "getGenericType"))),
										AssignExpr.Operator.ASSIGN))
								.addStatement(new AssignExpr(new NameExpr("a"), new MethodCallExpr(new NameExpr("f"), "getAnnotations"), AssignExpr.Operator.ASSIGN)),
						Utils.list(new CatchClause(new com.github.javaparser.ast.body.Parameter(types.getClass(Exception.class), "e"),
								new BlockStmt().addStatement(new AssignExpr(new NameExpr("t"), new ClassExpr(types.get(t1.name())), AssignExpr.Operator.ASSIGN))
										.addStatement(new AssignExpr(new NameExpr("a"), Utils.array(types.getClass(Annotation.class), 0), AssignExpr.Operator.ASSIGN)))),
						null));

		if (p instanceof JaxrsBodyParam) {
			cl.addField(types.getClass(JaxrsEntityReader.class, types.get(p.type)), n, Utils.PSF);
			b.addStatement(
					new AssignExpr(new NameExpr(n),
							new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "reader",
									Utils.list(new ClassExpr(types.get(p.type.name())), new NameExpr("t"), new NameExpr("a"), new NullLiteralExpr())),
							AssignExpr.Operator.ASSIGN));
		} else {
			cl.addField(types.getClass(ParamConverter.class, types.get(t)), n, Utils.PSF);
			b.addStatement(new AssignExpr(new NameExpr(n), new MethodCallExpr(new TypeExpr(types.getClass(JaxrsContext.class)), "converter",
					Utils.list(new ClassExpr(types.get(t1.name())), new NameExpr("t"), new NameExpr("a"))), AssignExpr.Operator.ASSIGN));
		}
	}

	private Expression getParam(JaxrsParam<?> p) {
		if (p instanceof JaxrsBodyParam)
			return new MethodCallExpr(new NameExpr(converterVar.get(p)), "read", Utils.list(new NameExpr("r")));
		if (p instanceof JaxrsBeanParam)
			return new MethodCallExpr(beansVar.get(((JaxrsBeanParam<?>) p).clazz.name()), new NameExpr("r"));
		return JaxrsModel.getParam(p, types, converterVar);
	}
}
