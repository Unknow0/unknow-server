/**
 * 
 */
package unknow.server.maven.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import unknow.server.http.utils.EventManager;
import unknow.server.maven.Builder;
import unknow.server.maven.Descriptor;
import unknow.server.maven.Descriptor.LD;
import unknow.server.maven.Names;
import unknow.server.maven.SD;
import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class CreateEventManager extends Builder {
	@Override
	public void add(ClassOrInterfaceDeclaration cl, Descriptor descriptor, TypeCache types) {
		BlockStmt init = cl.addMethod("createEventManager", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL).setType(types.get(EventManager.class)).getBody().get();
		Map<Class<?>, NodeList<Expression>> map = new HashMap<>();

		for (LD l : descriptor.listeners) {
			ClassOrInterfaceType t = types.get(l.t);
			Expression e = new ObjectCreationExpr(null, t, emptyList());

			for (SD s : descriptor.servlets) {
				if (s.e == l.t) {
					e = new CastExpr(t, new ArrayAccessExpr(new MethodCallExpr(Names.SERVLETS, "getServlets"), new IntegerLiteralExpr(Integer.toString(s.index))));
					break;
				}
			}
			for (Class<?> c : l.listener) {
				NodeList<Expression> ll = map.get(c);
				if (ll == null)
					map.put(c, ll = new NodeList<>());
				ll.add(e);
			}
		}

		NodeList<Expression> list = new NodeList<>();
		for (Class<?> l : Descriptor.LISTENERS) {
			Expression a;
			NodeList<Expression> ll = map.get(l);
			if (ll != null)
				a = new MethodCallExpr(new TypeExpr(types.get(Arrays.class)), "asList", ll);
			else
				a = new IntegerLiteralExpr("0");

			list.add(new ObjectCreationExpr(null, types.get(ArrayList.class, TypeCache.EMPTY), list(a)));
		}
		init.addStatement(new ReturnStmt(new ObjectCreationExpr(null, types.get(EventManager.class), list)));
	}
}
