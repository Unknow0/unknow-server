/**
 * 
 */
package unknow.server.maven.builder;

import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.github.javaparser.ast.Modifier;
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
import unknow.server.maven.descriptor.SD;

/**
 * @author unknow
 */
public class Initialize extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		Collections.sort(ctx.descriptor().servlets, (a, b) -> a.loadOnStartup - b.loadOnStartup);

		TypeCache t = ctx.type();
		BlockStmt b = ctx.self().addMethod("initialize", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).addThrownException(t.get(ServletException.class))
				.getBody().get();
		if (!ctx.descriptor().servlets.isEmpty()) {
			b.addStatement(assign(t.array(Servlet.class), "s", new MethodCallExpr(Names.SERVLETS, "getServlets")));
			for (SD s : ctx.descriptor().servlets)
				b.addStatement(new MethodCallExpr(new ArrayAccessExpr(Names.s, new IntegerLiteralExpr(Integer.toString(s.index))), "init", list(new ObjectCreationExpr(null, t.get(ServletConfigImpl.class), list(new StringLiteralExpr(s.name), Names.CTX, mapString(s.param, t))))));
		}

		if (!ctx.descriptor().filters.isEmpty()) {
			b.addStatement(assign(t.array(Filter.class), "f", new MethodCallExpr(Names.SERVLETS, "getFilters")));
			for (SD f : ctx.descriptor().filters)
				b.addStatement(new MethodCallExpr(new ArrayAccessExpr(Names.f, new IntegerLiteralExpr(Integer.toString(f.index))), "init", list(new ObjectCreationExpr(null, t.get(FilterConfigImpl.class), list(new StringLiteralExpr(f.name), Names.CTX, mapString(f.param, t))))));
		}
	}
}
