/**
 * 
 */
package unknow.server.maven.servlet.builder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.UnknownType;

import unknow.server.http.HttpHandler;
import unknow.server.maven.TypeCache;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;
import unknow.server.nio.Handler;
import unknow.server.nio.HandlerFactory;

/**
 * @author unknow
 */
public class Call extends Builder {
	@Override
	public void add(BuilderContext ctx) {
		TypeCache t = ctx.type();
		ctx.self().addMethod("call", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL).setType(Integer.class).addAnnotation(Override.class).addThrownException(Exception.class)
				.getBody().get()
				.addStatement(assign(t.get(ExecutorService.class), "executor",
						new ObjectCreationExpr(null, t.get(ThreadPoolExecutor.class), list(
								new NameExpr("execMin"), new NameExpr("execMax"), new NameExpr("execIdle"),
								new FieldAccessExpr(new TypeExpr(t.get(TimeUnit.class)), "SECONDS"),
								new ObjectCreationExpr(null, t.get(SynchronousQueue.class, t.get(Runnable.class)), emptyList()),
								new LambdaExpr(new Parameter(new UnknownType(), "r"),
										new BlockStmt()
												.addStatement(assign(t.get(Thread.class), "t", new ObjectCreationExpr(null, t.get(Thread.class), list(new NameExpr("r")))))
												.addStatement(new MethodCallExpr(new NameExpr("t"), "setDaemon", list(new BooleanLiteralExpr(true))))
												.addStatement(new ReturnStmt(new NameExpr("t"))))))))
				.addStatement(new AssignExpr(new NameExpr("handler"), new ObjectCreationExpr(null, t.get(HandlerFactory.class), null, emptyList(), list(
						new MethodDeclaration(Modifier.createModifierList(Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL), t.get(Handler.class), "create").addAnnotation(Override.class)
								.setBody(new BlockStmt()
										.addStatement(new ReturnStmt(new ObjectCreationExpr(null, t.get(HttpHandler.class), list(new NameExpr("executor"), new ThisExpr(new Name(ctx.self().getName().getIdentifier())), new NameExpr("keepAliveIdle")))))))),
						Operator.ASSIGN))
				.addStatement(new MethodCallExpr("loadInitializer"))
				.addStatement(new MethodCallExpr("initialize"))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireContextInitialized", list(Names.CTX)))
				.addStatement(assign(t.get(Integer.class), "err", new MethodCallExpr(new SuperExpr(), "call")))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireContextDestroyed", list(Names.CTX)))
				.addStatement(new ReturnStmt(new NameExpr("err")));
	}
}
