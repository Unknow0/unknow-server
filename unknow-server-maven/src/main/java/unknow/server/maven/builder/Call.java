/**
 * 
 */
package unknow.server.maven.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;

import unknow.server.maven.Builder;
import unknow.server.maven.Descriptor;
import unknow.server.maven.Names;
import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class Call extends Builder {
	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		cl.addMethod("call", Modifier.Keyword.PUBLIC, Modifier.Keyword.FINAL).setType(Integer.class).addAnnotation(Override.class).addThrownException(Exception.class)
				.getBody().get()
				.addStatement(new MethodCallExpr("loadInitializer"))
				.addStatement(new MethodCallExpr("initialize"))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireContextInitialized", list(Names.CTX)))
				.addStatement(assign(types.get(Integer.class), "err", new MethodCallExpr(new SuperExpr(), "call")))
				.addStatement(new MethodCallExpr(Names.EVENTS, "fireContextDestroyed", list(Names.CTX)))
				.addStatement(new ReturnStmt(new NameExpr("err")));
	}
}
