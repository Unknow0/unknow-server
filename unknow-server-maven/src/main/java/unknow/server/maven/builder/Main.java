/**
 * 
 */
package unknow.server.maven.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;

import picocli.CommandLine;
import unknow.server.maven.Builder;
import unknow.server.maven.TypeCache;
import unknow.server.maven.descriptor.Descriptor;

/**
 * @author unknow
 */
public class Main extends Builder {

	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		cl.addMethod("main", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC)
				.addParameter(String[].class, "arg")
				.getBody().get()
				.addStatement(new MethodCallExpr(new TypeExpr(types.get(System.class)), "exit").addArgument(new MethodCallExpr(new ObjectCreationExpr(null, types.get(CommandLine.class), list(new ObjectCreationExpr(null, types.get(cl), emptyList()))), "execute").addArgument(new NameExpr("arg"))));
	}
}
