/**
 * 
 */
package unknow.server.maven.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;

import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.maven.Builder;
import unknow.server.maven.Names;
import unknow.server.maven.TypeCache;
import unknow.server.maven.descriptor.Descriptor;

/**
 * @author unknow
 */
public class CreateContext extends Builder {

	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		cl.addMethod("createContext", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).setType(types.get(ServletContextImpl.class))
				.getBody().get()
				.addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.get(ServletContextImpl.class), list(new StringLiteralExpr(descriptor.name), mapString(descriptor.param, types), Names.SERVLETS, Names.EVENTS))));
	}
}
