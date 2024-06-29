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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;

import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.descriptor.Descriptor;
import unknow.server.maven.servlet.descriptor.LD;
import unknow.server.servlet.utils.EventManager;

/**
 * @author unknow
 */
public class CreateEventManager extends Builder {
	@Override
	public void add(BuilderContext ctx) {
		TypeCache t = ctx.type();
		BlockStmt init = ctx.self().addMethod("createEventManager", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).setType(t.getClass(EventManager.class))
				.addMarkerAnnotation(Override.class).createBody();
		Map<Class<?>, NodeList<Expression>> map = new HashMap<>();

		int i = 0;
		for (LD l : ctx.descriptor().listeners) {
			String n = "l" + i++;
			NameExpr name = new NameExpr(n);
			init.addStatement(Utils.assign(t.getClass(l.clazz), n, new ObjectCreationExpr(null, t.getClass(l.clazz), Utils.list())));

			for (Class<?> cl : l.listener) {
				NodeList<Expression> ll = map.get(cl);
				if (ll == null)
					map.put(cl, ll = new NodeList<>());
				ll.add(name);
			}
		}

		NodeList<Expression> list = new NodeList<>();
		for (Class<?> l : Descriptor.LISTENERS) {
			Expression a;
			NodeList<Expression> ll = map.get(l);
			if (ll != null)
				a = new MethodCallExpr(new TypeExpr(t.getClass(Arrays.class)), "asList", ll);
			else
				a = new IntegerLiteralExpr("0");

			list.add(new ObjectCreationExpr(null, t.getClass(ArrayList.class, TypeCache.EMPTY), Utils.list(a)));
		}
		init.addStatement(new ReturnStmt(new ObjectCreationExpr(null, t.getClass(EventManager.class), list)));
	}
}
