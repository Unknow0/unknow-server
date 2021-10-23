/**
 * 
 */
package unknow.server.maven.servlet.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;

/**
 * servlet of filter descriptor
 * 
 * @author unknow
 */
public class SD {
	public final int index;
	public String clazz;
	public String jsp;

	public String name;
	public final List<String> pattern = new ArrayList<>();
	public final Map<String, String> param = new HashMap<>();
	public int loadOnStartup = -1;
	public final List<String> servletNames = new ArrayList<>(0);
	public final List<DispatcherType> dispatcher = new ArrayList<>(0);
	public boolean enabled = true;

	public SD(int index) {
		this.index = index;
	}

	public SD(int index, AnnotationExpr a, ClassOrInterfaceDeclaration e) {
		this.index = index;
		this.clazz = e.resolve().getQualifiedName();
		String ln = null;
		for (Node n : a.getChildNodes()) {
			if (!(n instanceof MemberValuePair))
				continue;
			MemberValuePair m = (MemberValuePair) n;
			String k = m.getName().getIdentifier();
			if ("value".equals(k) || "urlPatterns".equals(k))
				add(pattern, m.getValue());
			else if ("loadOnStartup".equals(k))
				loadOnStartup = m.getValue().asIntegerLiteralExpr().asNumber().intValue();
			else if ("initParams".equals(k)) {
				String key = m.getValue().asAnnotationExpr().findFirst(MemberValuePair.class, w -> "name".equals(w.getName().getIdentifier())).get().getValue().asStringLiteralExpr().getValue();
				String value = m.getValue().asAnnotationExpr().findFirst(MemberValuePair.class, w -> "value".equals(w.getName().getIdentifier())).get().getValue().asStringLiteralExpr().getValue();
				param.put(key, value);
			} else if ("name".equals(k) || "filterName".equals(k))
				ln = m.getValue().asStringLiteralExpr().getValue();
			else if ("servletNames".equals(k))
				add(servletNames, m.getValue());
			else if ("dispatcherTypes".equals(k))
				parseDispatcher(m.getValue());
		}
		this.name = ln != null ? ln : e.resolve().getQualifiedName();
	}

	private void parseDispatcher(Expression e) {
		List<Expression> list;
		if (e.isArrayInitializerExpr())
			list = e.asArrayInitializerExpr().getValues();
		else
			list = Arrays.asList(e);

		for (Expression value : list) {
			String n;
			if (value.isFieldAccessExpr())
				n = value.asFieldAccessExpr().getNameAsString();
			else
				n = value.asNameExpr().getNameAsString();
			dispatcher.add(DispatcherType.valueOf(n));
		}
	}

	private static void add(List<String> list, Expression e) {
		List<Expression> values;
		if (e.isStringLiteralExpr())
			values = Arrays.asList(e);
		else
			values = e.asArrayInitializerExpr().getValues();
		for (Expression v : values)
			list.add(v.asStringLiteralExpr().getValue());
	}

	@Override
	public String toString() {
		return name == null ? clazz : name + (dispatcher.isEmpty() ? "" : dispatcher);
	}
}