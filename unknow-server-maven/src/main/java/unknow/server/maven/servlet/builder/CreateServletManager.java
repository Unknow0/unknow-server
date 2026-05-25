/**
 * 
 */
package unknow.server.maven.servlet.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.PrimitiveType;

import unknow.maven.codegen.CodeGenUtils;
import unknow.maven.codegen.TypeFactory;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;
import unknow.server.maven.servlet.descriptor.Descriptor;
import unknow.server.maven.servlet.descriptor.SD;
import unknow.server.servlet.utils.ServletManager;
import unknow.server.util.data.IntArrayMap;
import unknow.server.util.data.ObjectArrayMap;

/**
 * @author unknow
 */
public class CreateServletManager extends Builder {
	@Override
	public void add(BuilderContext ctx) {
		Descriptor descriptor = ctx.descriptor();
		TypeFactory types = ctx.type();
		ctx.self().addMethod("createServletManager", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).setType(types.getClass(ServletManager.class))
				.addMarkerAnnotation(Override.class).createBody().addStatement(new ReturnStmt(
						new ObjectCreationExpr(null, types.getClass(ServletManager.class), CodeGenUtils.list(errorCode(descriptor, types), errorClass(descriptor, types)))));
	}

	private static ObjectCreationExpr errorCode(Descriptor descriptor, TypeFactory t) {
		NodeList<Expression> k = new NodeList<>();
		NodeList<Expression> v = new NodeList<>();
		List<Integer> l = new ArrayList<>(descriptor.errorCode.keySet());
		Collections.sort(l);
		for (Integer e : l) {
			String path = descriptor.errorCode.get(e);
			SD s = descriptor.findServlet(path);
			if (s == null)
				continue;
			k.add(new IntegerLiteralExpr(e.toString()));
			v.add(CodeGenUtils.text(path));
		}
		return new ObjectCreationExpr(null, t.getClass(IntArrayMap.class, TypeFactory.EMPTY),
				CodeGenUtils.list(CodeGenUtils.array(PrimitiveType.intType(), k), CodeGenUtils.array(t.getClass(String.class), v)));
	}

	private static ObjectCreationExpr errorClass(Descriptor descriptor, TypeFactory t) {
		NodeList<Expression> k = new NodeList<>();
		NodeList<Expression> v = new NodeList<>();
		List<String> l = new ArrayList<>(descriptor.errorClass.keySet());
		Collections.sort(l);
		for (String e : l) {
			String path = descriptor.errorClass.get(e);
			SD s = descriptor.findServlet(path);
			if (s == null || e.isEmpty())
				continue;
			k.add(new ClassExpr(t.get(e.toString())));
			k.add(CodeGenUtils.text(path));
		}
		LambdaExpr cmp = new LambdaExpr(CodeGenUtils.list(new Parameter(TypeFactory.EMPTY, "a"), new Parameter(TypeFactory.EMPTY, "b")),
				new MethodCallExpr(new MethodCallExpr(Names.a, "getName"), "compareTo", CodeGenUtils.list(new MethodCallExpr(Names.b, "getName"))));
		return new ObjectCreationExpr(null, t.getClass(ObjectArrayMap.class, TypeFactory.EMPTY),
				CodeGenUtils.list(CodeGenUtils.array(t.getClass(Class.class), k), CodeGenUtils.array(t.getClass(String.class), v), cmp));
	}
}
