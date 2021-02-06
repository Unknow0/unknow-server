/**
 * 
 */
package unknow.server.maven.builder;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.FilterChain;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.TryStmt;

import unknow.server.http.HttpError;
import unknow.server.http.HttpRawRequest;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.servlet.ServletResponseImpl;
import unknow.server.maven.Builder;
import unknow.server.maven.Descriptor;
import unknow.server.maven.Names;
import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class Process extends Builder {

	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		cl.addMethod("process", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL)
				.addAnnotation(Override.class).addThrownException(types.get(IOException.class))
				.addParameter(types.get(HttpRawRequest.class), "request").addParameter(types.get(OutputStream.class), "out")
				.getBody().get()
				.addStatement(assign(types.get(ServletRequestImpl.class), "req", new ObjectCreationExpr(null, types.get(ServletRequestImpl.class), list(Names.CTX, Names.request))))

				.addStatement(new MethodCallExpr(Names.EVENTS, "fireRequestInitialized", list(Names.req)))
				.addStatement(assign(types.get(FilterChain.class), "s", new MethodCallExpr(Names.SERVLETS, "find", list(Names.req))))

				.addStatement(new IfStmt(
						new BinaryExpr(Names.s, new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
						new BlockStmt()
								.addStatement(new MethodCallExpr(Names.out, "write", list(new FieldAccessExpr(new FieldAccessExpr(new TypeExpr(types.get(HttpError.class)), "NOT_FOUND"), "data"))))
								.addStatement(new MethodCallExpr(Names.out, "close"))
								.addStatement(new MethodCallExpr(Names.EVENTS, "fireRequestDestroyed", list(new NameExpr("req"))))
								.addStatement(new ReturnStmt()),
						null))

				.addStatement(assign(types.get(ServletResponseImpl.class), "res", new ObjectCreationExpr(null, types.get(ServletResponseImpl.class), list(Names.CTX, Names.out))))
				.addStatement(new TryStmt(
						new BlockStmt()
								.addStatement(new MethodCallExpr(Names.s, "doFilter", list(Names.req, Names.res))),
						list(
								new CatchClause(new Parameter(types.get(Exception.class), "e"), new BlockStmt()
										.addStatement(new MethodCallExpr(Names.log, "error", list(new StringLiteralExpr("failed to service '{}'"), Names.e, Names.s)))
										.addStatement(new MethodCallExpr(Names.out, "write", list(
												new FieldAccessExpr(new FieldAccessExpr(new TypeExpr(types.get(HttpError.class)), "SERVER_ERROR"), "data")))))),
						null))

				.addStatement(new MethodCallExpr(Names.EVENTS, "fireRequestDestroyed", list(new NameExpr("req"))))
				.addStatement(new MethodCallExpr(Names.out, "close"));
	}
}
