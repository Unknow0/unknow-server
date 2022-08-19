/**
 * 
 */
package unknow.server.maven.servlet.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import jakarta.servlet.DispatcherType;
import unknow.server.http.data.ArraySet;
import unknow.server.http.servlet.FilterConfigImpl;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;
import unknow.server.maven.servlet.descriptor.Descriptor;
import unknow.server.maven.servlet.descriptor.SD;

/**
 * @author unknow
 */
public class CreateFilters extends Builder {
	@Override
	public void add(BuilderContext ctx) {
		Descriptor descriptor = ctx.descriptor();
		TypeCache types = ctx.type();
		ClassOrInterfaceType t = types.get(FilterConfigImpl.class);
		BlockStmt b = ctx.self().addMethod("createFilters", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).setType(types.array(FilterConfigImpl.class))
				.addMarkerAnnotation(Override.class)
				.getBody().get();

		NodeList<Expression> filters = new NodeList<>();
		for (SD f : descriptor.filters) {
			String n = "f" + f.index;
			filters.add(new NameExpr(n));

			NodeList<Expression> list = new NodeList<>();
			TypeExpr type = new TypeExpr(types.get(DispatcherType.class));
			for (DispatcherType d : f.dispatcher)
				list.add(new FieldAccessExpr(type, d.name()));
			Expression dispatcher = new ObjectCreationExpr(null, types.get(ArraySet.class, TypeCache.EMPTY), Utils.list(Utils.array(types.get(DispatcherType.class), list)));

			b.addStatement(Utils.assign(t, n, new ObjectCreationExpr(null, t, Utils.list(
					new StringLiteralExpr(f.name),
					new ObjectCreationExpr(null, types.get(f.clazz), Utils.list()),
					Names.CTX,
					Utils.mapString(f.param, types),
					Utils.arraySet(f.servletNames, types), Utils.arraySet(f.pattern, types), dispatcher))));
		}

		b.addStatement(new ReturnStmt(Utils.array(t, filters)));
	}
}
