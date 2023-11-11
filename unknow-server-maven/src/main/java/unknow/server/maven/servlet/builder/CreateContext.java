/**
 * 
 */
package unknow.server.maven.servlet.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;

/**
 * @author unknow
 */
public class CreateContext extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		TypeCache t = ctx.type();

		ctx.self().addMethod("createContext", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).setType(t.getClass(ServletContextImpl.class))
				.addMarkerAnnotation(Override.class).createBody()
				.addStatement(new IfStmt(new BinaryExpr(new NameExpr("vhost"), new NullLiteralExpr(), Operator.EQUALS),
						new ExpressionStmt(new AssignExpr(new NameExpr("vhost"),
								new ConditionalExpr(new BinaryExpr(new NameExpr("address"), new NullLiteralExpr(), Operator.EQUALS), Utils.text("localhost"),
										new NameExpr("address")),
								AssignExpr.Operator.ASSIGN)),
						null))
				.addStatement(new ReturnStmt(new ObjectCreationExpr(null, t.getClass(ServletContextImpl.class),
						Utils.list(Utils.text(ctx.descriptor().name), new NameExpr("vhost"), Utils.mapString(ctx.descriptor().param, t), Names.SERVLETS, Names.EVENTS,
								new ObjectCreationExpr(null, t.getClass(ctx.sessionFactory()), Utils.list()), Utils.mapString(ctx.descriptor().localeMapping, t),
								Utils.mapString(ctx.descriptor().mimeTypes, t)))));
	}
}
