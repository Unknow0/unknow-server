/**
 * 
 */
package unknow.server.maven.builder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.UnknownType;

import picocli.CommandLine;
import unknow.server.http.HttpHandler;
import unknow.server.maven.Builder;
import unknow.server.maven.Descriptor;
import unknow.server.maven.TypeCache;
import unknow.server.nio.Handler;
import unknow.server.nio.HandlerFactory;

/**
 * @author unknow
 */
public class Main extends Builder {

	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		MethodDeclaration annonCreate = new MethodDeclaration(Modifier.createModifierList(Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL), types.get(Handler.class), "create").addAnnotation(Override.class);
		annonCreate.getBody().get().addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.get(HttpHandler.class), list(new NameExpr("executor"), new NameExpr("c")))));

		cl.addMethod("main", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
				.addParameter(String[].class, "arg")
				.getBody().get()
				.addStatement(assign(types.get(cl), "c", new ObjectCreationExpr(null, types.get(cl), emptyList())))
				.addStatement(assign(types.get(ExecutorService.class), "executor",
						new MethodCallExpr(new TypeExpr(types.get(Executors.class)), "newCachedThreadPool").addArgument(new LambdaExpr(new Parameter(new UnknownType(), "r"),
								new BlockStmt()
										.addStatement(assign(types.get(Thread.class), "t", new ObjectCreationExpr(null, types.get(Thread.class), list(new NameExpr("r")))))
										.addStatement(new MethodCallExpr(new NameExpr("t"), "setDaemon", list(new BooleanLiteralExpr(true))))
										.addStatement(new ReturnStmt(new NameExpr("t")))))))
				.addStatement(new AssignExpr(new FieldAccessExpr(new NameExpr("c"), "handler"), new ObjectCreationExpr(null, types.get(HandlerFactory.class), null, emptyList(), list(annonCreate)), Operator.ASSIGN))
				.addStatement(new MethodCallExpr(new TypeExpr(types.get(System.class)), "exit").addArgument(new MethodCallExpr(new ObjectCreationExpr(null, types.get(CommandLine.class), list(new NameExpr("c"))), "execute").addArgument(new NameExpr("arg"))));
	}
}
