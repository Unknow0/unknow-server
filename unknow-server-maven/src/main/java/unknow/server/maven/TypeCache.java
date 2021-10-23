/**
 * 
 */
package unknow.server.maven;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;

/**
 * @author unknow
 */
public class TypeCache {
	public static final Type EMPTY = new UnknownType();

	private final Map<String, ClassOrInterfaceType> types;

	private final CompilationUnit cu;
	private final Map<String, String> existingClass;

	private final StringBuilder sb;

	public TypeCache(CompilationUnit cu, Map<String, String> existingClass) {
		this.cu = cu;
		this.existingClass = new HashMap<>(existingClass);

		this.types = new HashMap<>();
		this.sb = new StringBuilder();
	}

	public ArrayType array(Class<?> cl, Type... param) {
		return new ArrayType(get(cl, param));
	}

	public ClassOrInterfaceType get(ClassOrInterfaceDeclaration decl) {
		ResolvedReferenceTypeDeclaration resolve = decl.resolve();
		String n = resolve.getQualifiedName();
		ClassOrInterfaceType t = types.get(n);
		if (t == null) {
			String string = existingClass.get(decl.getNameAsString());
			if (string == null || string.equals(n)) {
				if (string == null) {
					cu.addImport(n);
					existingClass.put(decl.getNameAsString(), n);
				}
				t = new ClassOrInterfaceType(null, decl.getName(), null);
			} else {
				for (String s : n.split("[.$]"))
					t = new ClassOrInterfaceType(t, s);
			}
			types.put(n, t);
		}
		return t;
	}

	public ClassOrInterfaceType get(Class<?> cl, Type... param) {
		sb.append(cl.getCanonicalName());
		if (param.length > 0) {
			sb.append('<');
			for (int i = 0; i < param.length; i++) {
				Type t = param[i];
				if (t != EMPTY)
					sb.append(t.asString());
			}
			sb.append('>');
		}
		ClassOrInterfaceType c = types.get(sb.toString());
		if (c == null)
			types.put(sb.toString(), c = create(cl, param));
		sb.setLength(0);
		return c;
	}

	public ClassOrInterfaceType get(String cl) {
		int indexOf = cl.indexOf('<');
		if (indexOf >= 0)
			cl = cl.substring(0, indexOf);
		ClassOrInterfaceType t = types.get(cl);
		if (t != null)
			return t;

		String[] split = cl.split("[.$]");
		String last = split[split.length - 1];
		String string = existingClass.get(last);
		if (string != null && !cl.equals(string)) {
			for (int i = 0; i < split.length; i++)
				t = new ClassOrInterfaceType(t, split[i]);
		} else {
			if (split.length > 1 && string == null) {
				cu.addImport(cl.replace('$', '.'));
				existingClass.put(last, cl);
			}
			t = new ClassOrInterfaceType(null, last);
		}
		types.put(cl, t);
		return t;
	}

	@SuppressWarnings("null")
	private ClassOrInterfaceType create(Class<?> cl, Type... param) {
		ClassOrInterfaceType r = null;
		String string = existingClass.get(cl.getSimpleName());
		if (string == null || string.equals(cl.getName())) {
			if (string == null) {
				cu.addImport(cl);
				existingClass.put(cl.getSimpleName(), cl.getName());
			}
			r = new ClassOrInterfaceType(null, cl.getSimpleName());
		} else {
			for (String s : cl.getName().split("[.$]")) {
				r = new ClassOrInterfaceType(r, s);
			}
		}
		NodeList<Type> p = null;
		if (param.length > 0) {
			p = new NodeList<>();
			for (int i = 0; i < param.length; i++) {
				Type t = param[i];
				if (t != EMPTY)
					p.add(t);
			}
		}
		return r.setTypeArguments(p);
	}
}