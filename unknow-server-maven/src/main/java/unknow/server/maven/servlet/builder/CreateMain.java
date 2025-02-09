/**
 * 
 */
package unknow.server.maven.servlet.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;

/**
 * @author unknow
 */
public class CreateMain extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		TypeCache t = ctx.type();
		ctx.self().addMethod("main", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC).addParameter(String[].class, "arg").addThrownException(t.getClass(Exception.class))
				.createBody().addStatement(new MethodCallExpr(new ObjectCreationExpr(null, t.getClass(ctx.self()), Utils.list()), "process", Utils.list(new NameExpr("arg"))));
	}
}
