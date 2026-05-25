/**
 * 
 */
package unknow.server.maven.servlet.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import unknow.maven.codegen.CodeGenUtils;
import unknow.maven.codegen.TypeFactory;
import unknow.server.maven.servlet.Builder;

/**
 * @author unknow
 */
public class Main extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		TypeFactory t = ctx.type();
		ctx.self().addMethod("main", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC).addParameter(String[].class, "arg").addThrownException(t.getClass(Exception.class))
				.createBody().addStatement(new MethodCallExpr(new ObjectCreationExpr(null, t.getClass(ctx.self()), CodeGenUtils.list()), "process", CodeGenUtils.list(new NameExpr("arg"))));
	}
}
