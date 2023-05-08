/**
 * 
 */
package unknow.server.maven.servlet.builder;

import java.util.HashSet;
import java.util.Set;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import unknow.server.http.servlet.ServletConfigImpl;
import unknow.server.http.utils.Resource;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;
import unknow.server.maven.servlet.Names;
import unknow.server.maven.servlet.descriptor.Descriptor;
import unknow.server.maven.servlet.descriptor.SD;

/**
 * @author unknow
 */
public class CreateServlets extends Builder {
	@Override
	public void add(BuilderContext ctx) {
		Descriptor descriptor = ctx.descriptor();
		TypeCache types = ctx.type();
		ClassOrInterfaceType t = types.getClass(ServletConfigImpl.class);
		BlockStmt b = ctx.self().addMethod("createServlets", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).setType(types.array(ServletConfigImpl.class))
				.addMarkerAnnotation(Override.class)
				.getBody().get();

		NodeList<Expression> servlets = new NodeList<>();

		Set<String> saw = new HashSet<>();
		for (SD s : descriptor.servlets) {
			String n = "s" + s.index;
			servlets.add(new NameExpr(n));

			NodeList<Expression> params = new NodeList<>();
			if (s.name.startsWith("Resource:")) {
				Resource r = descriptor.resources.get(s.pattern.get(0));
				params = Utils.list(new StringLiteralExpr(s.pattern.get(0)), new LongLiteralExpr(r.getLastModified() + "L"), new LongLiteralExpr(r.getSize() + "L"));
			}
			b.addStatement(Utils.assign(t, n, new ObjectCreationExpr(null, t, Utils.list(new StringLiteralExpr(s.name), new ObjectCreationExpr(null, types.getClass(s.clazz), params), Names.CTX, Utils.mapString(s.param, types), Utils.arraySet(s.pattern, types)))));
			for (String p : s.pattern) {
				if (saw.contains(p))
					System.err.println("duplicate servlet pattern '" + p + "'");
				saw.add(p);
			}
		}

		b.addStatement(new ReturnStmt(Utils.array(t, servlets)));
	}
}
