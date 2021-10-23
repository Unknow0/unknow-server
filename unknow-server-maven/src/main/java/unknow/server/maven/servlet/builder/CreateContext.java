/**
 * 
 */
package unknow.server.maven.servlet.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;

import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.maven.TypeCache;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;

/**
 * @author unknow
 */
public class CreateContext extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		TypeCache t = ctx.type();

		ctx.self().addMethod("createContext", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).setType(t.get(ServletContextImpl.class))
				.getBody().get()
				.addStatement(new ReturnStmt(new ObjectCreationExpr(null, t.get(ServletContextImpl.class), list(
						new StringLiteralExpr(ctx.descriptor().name),
						mapString(ctx.descriptor().param, t),
						Names.SERVLETS,
						Names.EVENTS,
						new ObjectCreationExpr(null, t.get(ctx.sessionFactory()), emptyList()),
						mapString(ctx.descriptor().localeMapping, t),
						mapString(ctx.descriptor().mimeTypes, t)))));
	}
}
