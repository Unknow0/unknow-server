/**
 * 
 */
package unknow.server.maven.builder;

import java.util.Collections;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import unknow.server.http.servlet.FilterConfigImpl;
import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.utils.ArrayMap;
import unknow.server.maven.Builder;
import unknow.server.maven.Descriptor;
import unknow.server.maven.Names;
import unknow.server.maven.SD;
import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class Initialize extends Builder {

	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		Collections.sort(descriptor.servlets, (a, b) -> a.loadOnStartup - b.loadOnStartup);

		BlockStmt b = cl.addMethod("initialize", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).addThrownException(types.get(ServletException.class))
				.getBody().get()
				.addStatement(assign(types.array(Servlet.class), "s", new MethodCallExpr(Names.SERVLETS, "getServlets")));

		for (SD s : descriptor.servlets) {
			NodeList<Expression> k = new NodeList<>();
			NodeList<Expression> v = new NodeList<>();
			for (Entry<String, String> e : s.param.entrySet()) {
				k.add(new StringLiteralExpr(e.getKey()));
				v.add(new StringLiteralExpr(e.getValue()));
			}
			ObjectCreationExpr p = new ObjectCreationExpr(null, types.get(ArrayMap.class, TypeCache.EMPTY), list(array(types.get(String.class), k), array(types.get(String.class), v)));
			b.addStatement(new MethodCallExpr(new ArrayAccessExpr(Names.s, new IntegerLiteralExpr(Integer.toString(s.index))), "init", list(new ObjectCreationExpr(null, types.get(ServletConfigImpl.class), list(new StringLiteralExpr(s.name), Names.CTX, p)))));
		}

		b.addStatement(assign(types.array(Filter.class), "f", new MethodCallExpr(Names.SERVLETS, "getFilters")));
		for (SD f : descriptor.filters) {
			NodeList<Expression> k = new NodeList<>();
			NodeList<Expression> v = new NodeList<>();
			for (Entry<String, String> e : f.param.entrySet()) {
				k.add(new StringLiteralExpr(e.getKey()));
				v.add(new StringLiteralExpr(e.getValue()));
			}
			ObjectCreationExpr p = new ObjectCreationExpr(null, types.get(ArrayMap.class, TypeCache.EMPTY), list(array(types.get(String.class), k), array(types.get(String.class), v)));
			b.addStatement(new MethodCallExpr(new ArrayAccessExpr(Names.f, new IntegerLiteralExpr(Integer.toString(f.index))), "init", list(new ObjectCreationExpr(null, types.get(FilterConfigImpl.class), list(new StringLiteralExpr(f.name), Names.CTX, p)))));
		}
	}
}
