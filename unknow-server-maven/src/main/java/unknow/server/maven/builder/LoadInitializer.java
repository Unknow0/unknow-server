/**
 * 
 */
package unknow.server.maven.builder;

import java.util.ServiceLoader;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;

import unknow.server.maven.Builder;
import unknow.server.maven.Descriptor;
import unknow.server.maven.Names;
import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class LoadInitializer extends Builder {

	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		cl.addMethod("loadInitializer", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).addThrownException(types.get(ServletException.class))
				.getBody().get()
				.addStatement(new ForEachStmt(
						new VariableDeclarationExpr(types.get(ServletContainerInitializer.class), "i"),
						new MethodCallExpr(new TypeExpr(types.get(ServiceLoader.class)), "load", list(new ClassExpr(types.get(ServletContainerInitializer.class)))),
						new BlockStmt()
								// TODO HandleTypes annotation ?
								.addStatement(new MethodCallExpr(Names.i, "onStartup", list(new NullLiteralExpr(), Names.CTX)))));

	}
}
