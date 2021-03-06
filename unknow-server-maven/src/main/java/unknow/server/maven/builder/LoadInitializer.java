/**
 * 
 */
package unknow.server.maven.builder;

import java.util.ServiceLoader;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;

import unknow.server.maven.Builder;
import unknow.server.maven.Names;
import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class LoadInitializer extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		TypeCache t = ctx.type();
		ctx.self().addMethod("loadInitializer", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).addThrownException(t.get(ServletException.class))
				.getBody().get()
				.addStatement(new ForEachStmt(
						new VariableDeclarationExpr(t.get(ServletContainerInitializer.class), "i"),
						new MethodCallExpr(new TypeExpr(t.get(ServiceLoader.class)), "load", list(new ClassExpr(t.get(ServletContainerInitializer.class)))),
						new BlockStmt()
								// TODO HandleTypes annotation ?
								.addStatement(new MethodCallExpr(Names.i, "onStartup", list(new NullLiteralExpr(), Names.CTX)))));

	}
}
