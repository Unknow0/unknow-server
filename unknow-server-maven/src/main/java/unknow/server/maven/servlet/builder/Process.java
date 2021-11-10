/**
 * 
 */
package unknow.server.maven.servlet.builder;

import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.TryStmt;

import unknow.server.http.HttpHandler;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.servlet.ServletResponseImpl;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;

/**
 * @author unknow
 */
public class Process extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		StringLiteralExpr connection = new StringLiteralExpr("connection");
		TypeCache t = ctx.type();
		ctx.self().addMethod("process", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL)
				.addAnnotation(Override.class).addThrownException(t.get(IOException.class))
				.addParameter(t.get(HttpHandler.class), "request")
				.getBody().get()
				.addStatement(Utils.assign(t.get(ServletResponseImpl.class), "res", new ObjectCreationExpr(null, t.get(ServletResponseImpl.class), Utils.list(Names.CTX, new MethodCallExpr(Names.request, "getOut"), Names.request))))
				.addStatement(Utils.assign(t.get(ServletRequestImpl.class), "req", new ObjectCreationExpr(null, t.get(ServletRequestImpl.class), Utils.list(Names.CTX, Names.request, new FieldAccessExpr(new TypeExpr(t.get(DispatcherType.class)), "REQUEST"), Names.res))))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireRequestInitialized", Utils.list(Names.req)))
				.addStatement(Utils.assign(t.get(FilterChain.class), "s", new MethodCallExpr(Names.SERVLETS, "find", Utils.list(Names.req))))
				.addStatement(new IfStmt(
						new BinaryExpr(Names.s, new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
						new TryStmt(
								new BlockStmt()
										.addStatement(new MethodCallExpr(Names.s, "doFilter", Utils.list(Names.req, Names.res))),
								Utils.list(
										new CatchClause(new Parameter(t.get(Exception.class), "e"), new BlockStmt()
												.addStatement(new MethodCallExpr(Names.log, "error", Utils.list(new StringLiteralExpr("failed to service '{}'"), Names.s, Names.e)))
												.addStatement(new IfStmt(new UnaryExpr(new MethodCallExpr(Names.res, "isCommited"), UnaryExpr.Operator.LOGICAL_COMPLEMENT),
														new ExpressionStmt(new MethodCallExpr(Names.res, "sendError", Utils.list(new IntegerLiteralExpr("500"), new NullLiteralExpr()))),
														null)))),
								null),
						new ExpressionStmt(new MethodCallExpr(Names.res, "sendError", Utils.list(new IntegerLiteralExpr("404"), new NullLiteralExpr())))))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireRequestDestroyed", Utils.list(Names.req)))
				.addStatement(new IfStmt(
						new BinaryExpr(
								new BinaryExpr(new MethodCallExpr(Names.res, "getHeader", Utils.list(connection)), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
								new BinaryExpr(new MethodCallExpr(Names.req, "getHeader", Utils.list(connection)), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
								BinaryExpr.Operator.AND),
						new ExpressionStmt(new MethodCallExpr(Names.res, "setHeader", Utils.list(connection, new MethodCallExpr(Names.req, "getHeader", Utils.list(connection))))),
						null))
				.addStatement(new MethodCallExpr(Names.res, "close"));
	}
}
