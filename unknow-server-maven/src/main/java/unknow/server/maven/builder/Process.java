/**
 * 
 */
package unknow.server.maven.builder;

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
import unknow.server.maven.Builder;
import unknow.server.maven.Names;
import unknow.server.maven.TypeCache;

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
				.addStatement(assign(t.get(ServletResponseImpl.class), "res", new ObjectCreationExpr(null, t.get(ServletResponseImpl.class), list(Names.CTX, new MethodCallExpr(Names.request, "getOut"), Names.request))))
				.addStatement(assign(t.get(ServletRequestImpl.class), "req", new ObjectCreationExpr(null, t.get(ServletRequestImpl.class), list(Names.CTX, Names.request, new FieldAccessExpr(new TypeExpr(t.get(DispatcherType.class)), "REQUEST"), Names.res))))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireRequestInitialized", list(Names.req)))
				.addStatement(assign(t.get(FilterChain.class), "s", new MethodCallExpr(Names.SERVLETS, "find", list(Names.req))))
				.addStatement(new IfStmt(
						new BinaryExpr(Names.s, new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
						new TryStmt(
								new BlockStmt()
										.addStatement(new MethodCallExpr(Names.s, "doFilter", list(Names.req, Names.res))),
								list(
										new CatchClause(new Parameter(t.get(Exception.class), "e"), new BlockStmt()
												.addStatement(new MethodCallExpr(Names.log, "error", list(new StringLiteralExpr("failed to service '{}'"), Names.s, Names.e)))
												.addStatement(new IfStmt(new UnaryExpr(new MethodCallExpr(Names.res, "isCommited"), UnaryExpr.Operator.LOGICAL_COMPLEMENT),
														new ExpressionStmt(new MethodCallExpr(Names.res, "sendError", list(new IntegerLiteralExpr("500"), new NullLiteralExpr()))),
														null)))),
								null),
						new ExpressionStmt(new MethodCallExpr(Names.res, "sendError", list(new IntegerLiteralExpr("404"), new NullLiteralExpr())))))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireRequestDestroyed", list(Names.req)))
				.addStatement(new IfStmt(
						new BinaryExpr(
								new BinaryExpr(new MethodCallExpr(Names.res, "getHeader", list(connection)), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
								new BinaryExpr(new MethodCallExpr(Names.req, "getHeader", list(connection)), new NullLiteralExpr(), BinaryExpr.Operator.NOT_EQUALS),
								BinaryExpr.Operator.AND),
						new ExpressionStmt(new MethodCallExpr(Names.res, "setHeader", list(connection, new MethodCallExpr(Names.req, "getHeader", list(connection))))),
						null))
				.addStatement(new MethodCallExpr(Names.res, "close"));
	}
}
