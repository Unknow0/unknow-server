/**
 * 
 */
package unknow.server.maven.servlet.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;

import picocli.CommandLine;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;

/**
 * @author unknow
 */
public class Main extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		TypeCache t = ctx.type();
		ctx.self().addMethod("main", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
				.addParameter(String[].class, "arg")
				.getBody().get()
				.addStatement(new MethodCallExpr(new TypeExpr(t.getClass(System.class)), "exit").addArgument(new MethodCallExpr(new ObjectCreationExpr(null, t.getClass(CommandLine.class), Utils.list(new ObjectCreationExpr(null, t.getClass(ctx.self()), Utils.list()))), "execute").addArgument(new NameExpr("arg"))));
	}
}
