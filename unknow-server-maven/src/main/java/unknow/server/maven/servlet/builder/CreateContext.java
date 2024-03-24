/**
 * 
 */
package unknow.server.maven.servlet.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;

import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;
import unknow.server.servlet.impl.ServletContextImpl;

/**
 * @author unknow
 */
public class CreateContext extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		TypeCache t = ctx.type();

		ctx.self().addMethod("createContext", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).setType(t.getClass(ServletContextImpl.class))
				.addParameter(String.class, "vhost").addMarkerAnnotation(Override.class).createBody()
				.addStatement(new ReturnStmt(new ObjectCreationExpr(null, t.getClass(ServletContextImpl.class),
						Utils.list(Utils.text(ctx.descriptor().name), new NameExpr("vhost"), Utils.mapString(ctx.descriptor().param, t), Names.SERVLETS, Names.EVENTS,
								new ObjectCreationExpr(null, t.getClass(ctx.sessionFactory()), Utils.list()), Utils.mapString(ctx.descriptor().localeMapping, t),
								Utils.mapString(ctx.descriptor().mimeTypes, t)))));
	}
}
