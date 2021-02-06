/**
 * 
 */
package unknow.server.maven.builder;

import java.util.Map.Entry;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.utils.ArrayMap;
import unknow.server.maven.Builder;
import unknow.server.maven.Descriptor;
import unknow.server.maven.Names;
import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class CreateContext extends Builder {

	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		BlockStmt b = cl.addMethod("createContext", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).setType(types.get(ServletContextImpl.class))
				.getBody().get()
				.addStatement(assign(types.get(ArrayMap.class, types.get(String.class)), "initParam", new ObjectCreationExpr(null, types.get(ArrayMap.class, TypeCache.EMPTY), emptyList())));

		for (Entry<String, String> e : descriptor.param.entrySet())
			b.addStatement(new MethodCallExpr(Names.initParam, "put", list(new StringLiteralExpr(e.getKey()), new StringLiteralExpr(e.getValue()))));
		b.addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.get(ServletContextImpl.class), list(new StringLiteralExpr(descriptor.name), Names.initParam, Names.SERVLETS, Names.EVENTS))));
	}
}
