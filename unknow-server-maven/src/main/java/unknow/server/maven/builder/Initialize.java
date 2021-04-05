/**
 * 
 */
package unknow.server.maven.builder;

import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import unknow.server.http.servlet.FilterConfigImpl;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.maven.Builder;
import unknow.server.maven.Names;
import unknow.server.maven.TypeCache;
import unknow.server.maven.descriptor.Descriptor;
import unknow.server.maven.descriptor.SD;

/**
 * @author unknow
 */
public class Initialize extends Builder {

	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		Collections.sort(descriptor.servlets, (a, b) -> a.loadOnStartup - b.loadOnStartup);

		BlockStmt b = cl.addMethod("initialize", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).addThrownException(types.get(ServletException.class))
				.getBody().get();
		if (!descriptor.servlets.isEmpty()) {
			b.addStatement(assign(types.array(Servlet.class), "s", new MethodCallExpr(Names.SERVLETS, "getServlets")));
			for (SD s : descriptor.servlets)
				b.addStatement(new MethodCallExpr(new ArrayAccessExpr(Names.s, new IntegerLiteralExpr(Integer.toString(s.index))), "init", list(new ObjectCreationExpr(null, types.get(ServletConfigImpl.class), list(new StringLiteralExpr(s.name), Names.CTX, mapString(s.param, types))))));
		}

		if (!descriptor.filters.isEmpty()) {
			b.addStatement(assign(types.array(Filter.class), "f", new MethodCallExpr(Names.SERVLETS, "getFilters")));
			for (SD f : descriptor.filters)
				b.addStatement(new MethodCallExpr(new ArrayAccessExpr(Names.f, new IntegerLiteralExpr(Integer.toString(f.index))), "init", list(new ObjectCreationExpr(null, types.get(FilterConfigImpl.class), list(new StringLiteralExpr(f.name), Names.CTX, mapString(f.param, types))))));
		}
	}
}
