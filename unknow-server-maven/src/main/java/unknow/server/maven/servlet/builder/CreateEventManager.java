/**
 * 
 */
package unknow.server.maven.servlet.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
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
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;
import unknow.server.maven.servlet.descriptor.Descriptor;
import unknow.server.maven.servlet.descriptor.LD;
import unknow.server.maven.servlet.descriptor.SD;

/**
 * @author unknow
 */
public class CreateEventManager extends Builder {
	@Override
	public void add(BuilderContext ctx) {
		TypeCache t = ctx.type();
		BlockStmt init = ctx.self().addMethod("createEventManager", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).setType(t.get(EventManager.class))
				.addMarkerAnnotation(Override.class)
				.getBody().get();
		Map<Class<?>, NodeList<Expression>> map = new HashMap<>();

		for (LD l : ctx.descriptor().listeners) {
			ClassOrInterfaceType c = t.get(l.clazz);
			Expression e = new ObjectCreationExpr(null, c, Utils.list());

			for (SD s : ctx.descriptor().servlets) {
				if (s.clazz.equals(l.clazz)) {
					e = new CastExpr(c, new ArrayAccessExpr(new MethodCallExpr(Names.SERVLETS, "getServlets"), new IntegerLiteralExpr(Integer.toString(s.index))));
					break;
				}
			}
			for (Class<?> cl : l.listener) {
				NodeList<Expression> ll = map.get(cl);
				if (ll == null)
					map.put(cl, ll = new NodeList<>());
				ll.add(e);
			}
		}

		NodeList<Expression> list = new NodeList<>();
		for (Class<?> l : Descriptor.LISTENERS) {
			Expression a;
			NodeList<Expression> ll = map.get(l);
			if (ll != null)
				a = new MethodCallExpr(new TypeExpr(t.get(Arrays.class)), "asList", ll);
			else
				a = new IntegerLiteralExpr("0");

			list.add(new ObjectCreationExpr(null, t.get(ArrayList.class, TypeCache.EMPTY), Utils.list(a)));
		}
		init.addStatement(new ReturnStmt(new ObjectCreationExpr(null, t.get(EventManager.class), list)));
	}
}
