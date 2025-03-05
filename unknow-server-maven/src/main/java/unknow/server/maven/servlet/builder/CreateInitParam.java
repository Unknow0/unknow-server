/**
 * 
 */
package unknow.server.maven.servlet.builder;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;

import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;
import unknow.server.util.data.ArrayMap;

/**
 * @author unknow
 */
public class CreateInitParam extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		TypeCache t = ctx.type();

		ctx.self().addMethod("initParam", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).setType(t.getClass(ArrayMap.class, t.getClass(String.class)))
				.addMarkerAnnotation(Override.class).createBody().addStatement(new ReturnStmt(Utils.mapString(ctx.descriptor().localeMapping, t)));

		ctx.self().addMethod("contextName", Modifier.Keyword.PROTECTED, Modifier.Keyword.FINAL).setType(t.getClass(String.class)).addMarkerAnnotation(Override.class)
				.createBody().addStatement(new ReturnStmt(new StringLiteralExpr(ctx.descriptor().name)));
	}
}