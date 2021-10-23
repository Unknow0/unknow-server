/**
 * 
 */
package unknow.server.maven.servlet.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;

import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;

/**
 * @author unknow
 */
public class Constructor extends Builder {
	@Override
	public void add(BuilderContext ctx) {
		ctx.self().addConstructor(Modifier.Keyword.PRIVATE)
				.getBody()
				.addStatement(new AssignExpr(Names.SERVLETS, new MethodCallExpr(new ThisExpr(), "createServletManager"), Operator.ASSIGN))
				.addStatement(new AssignExpr(Names.EVENTS, new MethodCallExpr(new ThisExpr(), "createEventManager"), Operator.ASSIGN))
				.addStatement(new AssignExpr(Names.CTX, new MethodCallExpr(new ThisExpr(), "createContext"), Operator.ASSIGN));
	}
}
