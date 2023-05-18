/**
 * 
 */
package unknow.server.maven;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.WildcardType;

import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class TypeCache {
	public static final Type EMPTY = new UnknownType();
	public static final Type ANY = new WildcardType();

	private final Map<String, Type> types;

	private final CompilationUnit cu;
	private final Map<String, String> existingClass;

	public TypeCache(CompilationUnit cu, Map<String, String> existingClass) {
		this.cu = cu;
		this.existingClass = new HashMap<>(existingClass);

		this.types = new HashMap<>();
		types.put("", EMPTY);
		types.put("?", ANY);
	}

	public ClassOrInterfaceType getClass(String cl) {
		return get(cl).asClassOrInterfaceType();
	}

	public ClassOrInterfaceType getClass(TypeModel c) {
		return get(c.name()).asClassOrInterfaceType();
	}

	public ClassOrInterfaceType getClass(Class<?> c, Type... param) {
		String cl = c.getCanonicalName();
		if (param.length > 0) {
			StringBuilder sb = new StringBuilder(cl).append('<');
			for (int i = 0; i < param.length; i++)
				sb.append(param[i]).append(',');
			sb.setCharAt(sb.length() - 1, '>');
			cl = sb.toString();
		}
		return get(cl).asClassOrInterfaceType();
	}

	public ArrayType array(Class<?> cl) {
		return get(cl.getCanonicalName() + "[]").asArrayType();
	}

	public Type get(TypeModel c) {
		return get(c.toString());
	}

	public Type get(Class<?> c) {
		return get(c.getCanonicalName());
	}

	public ClassOrInterfaceType get(ClassOrInterfaceDeclaration decl) {
		return get(decl.resolve().getQualifiedName()).asClassOrInterfaceType();
	}

	public Type get(String cl) {
		Type t = types.get(cl);
		if (t == null)
			types.put(cl, t = create(cl));
		return t;
	}

	private Type create(String cl) {
		if (cl.endsWith("[]"))
			return new ArrayType(get(cl.substring(0, cl.length() - 2)));

		List<String> parse = ModelLoader.parse(cl);

		NodeList<Type> params = null;
		if (parse.size() > 1) {
			params = new NodeList<>();
			for (int i = 1; i < parse.size(); i++)
				params.add(get(parse.get(i)));
		}

		cl = parse.get(0);
		ClassOrInterfaceType t = null;
		String[] split = cl.split("[.$]");
		String last = split[split.length - 1];
		String string = existingClass.get(last);
		if (string != null && !cl.equals(string)) {
			for (int i = 0; i < split.length; i++)
				t = new ClassOrInterfaceType(t, split[i]);
			return t.setTypeArguments(params);
		}
		if (split.length > 1 && string == null) {
			cu.addImport(cl.replace('$', '.'));
			existingClass.put(last, cl);
		}
		return new ClassOrInterfaceType(null, new SimpleName(last), params);
	}
}