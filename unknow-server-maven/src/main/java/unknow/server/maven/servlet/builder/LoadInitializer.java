/**
 * 
 */
package unknow.server.maven.servlet.builder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import jakarta.servlet.ServletContainerInitializer;
import unknow.server.maven.Utils;
import unknow.server.maven.servlet.Builder;

/**
 * @author unknow
 */
public class LoadInitializer extends Builder {

	@Override
	public void add(BuilderContext ctx) {
		List<String> clazz = new ArrayList<>(ctx.descriptor().initializer);
		try {
			Enumeration<URL> e = ClassLoader.getSystemResources("/META-INF/services/" + ServletContainerInitializer.class.getName());
			while (e.hasMoreElements()) {
				URL nextElement = e.nextElement();
				try (BufferedReader br = new BufferedReader(new InputStreamReader(nextElement.openStream()))) {
					String l;
					while ((l = br.readLine()) != null)
						clazz.add(l);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		BlockStmt b = ctx.self().addMethod("loadInitializer", Keyword.PROTECTED, Keyword.FINAL).addMarkerAnnotation(Override.class).getBody().get();
		for (String s : clazz)
			b.addStatement(new MethodCallExpr(new ObjectCreationExpr(null, ctx.type().getClass(s), Utils.list()), "onStartup", Utils.list(new NullLiteralExpr(), new NameExpr("ctx"))));
	}
}
