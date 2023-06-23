/**
 * 
 */
package unknow.server.maven.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.ast.body.TypeDeclaration;

import unknow.server.maven.model.ast.AstClass;
import unknow.server.maven.model.ast.AstEnum;
import unknow.server.maven.model.jvm.JvmClass;
import unknow.server.maven.model.jvm.JvmEnum;

/**
 * @author unknow
 */
public class ModelLoader {
	private static final Map<String, TypeModel> BUILTIN = new HashMap<>();
	private static final TypeModel[] EMPTY = {};

	static {
		for (TypeModel t : Arrays.asList(VoidModel.SELF, PrimitiveModel.BOOLEAN, PrimitiveModel.BYTE, PrimitiveModel.CHAR, PrimitiveModel.SHORT, PrimitiveModel.INT,
				PrimitiveModel.LONG, PrimitiveModel.FLOAT, PrimitiveModel.DOUBLE))
			BUILTIN.put(t.name(), t);
	}

	private final ClassLoader cl;
	private final Map<String, TypeDeclaration<?>> classes;

	public ModelLoader(ClassLoader cl, Map<String, TypeDeclaration<?>> classes) {
		this.cl = cl;
		this.classes = classes;
	}

	public TypeModel get(String cl) {
		return get(cl, Collections.emptyList());
	}

	public TypeModel get(String cl, List<TypeParamModel> parameters) {
		TypeModel type = BUILTIN.get(cl);
		if (type != null)
			return type;
		if (cl.endsWith("[]"))
			return new ArrayModel(cl, get(cl.substring(0, cl.length() - 2), parameters));

		List<String> parse = parse(cl);
		Map<String, TypeModel> map = new HashMap<>();
		for (TypeParamModel t : parameters)
			map.put(t.name(), t.type());

		TypeModel[] params = EMPTY;
		if (parse.size() > 1) {
			cl = parse.get(0);
			params = new TypeModel[parse.size() - 1];
			for (int i = 1; i < parse.size(); i++) {
				String s = parse.get(i);
				params[i - 1] = map.containsKey(s) ? map.get(s) : get(s, parameters);
			}
		}

		type = map.get(cl);
		if (type != null)
			cl = type.name();

		TypeDeclaration<?> t = classes.get(cl);
		if (t != null) {
			if (t.isEnumDeclaration())
				return new AstEnum(this, t.asEnumDeclaration());
			else if (t.isClassOrInterfaceDeclaration())
				return new AstClass(this, t.asClassOrInterfaceDeclaration(), params);
			throw new RuntimeException("unsuported type " + t);
		}
		try {
			Class<?> c = this.cl.loadClass(cl);
			if (c.isEnum())
				return new JvmEnum(this, c, params);
			return new JvmClass(this, c, params);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	protected static final Pattern CLAZZ = Pattern.compile("(.+?)(?:<(.*?)>)?");
	protected static final Pattern CLAZZ_LIST = Pattern.compile("(.+?(?:<.*?>)?)(?:,|$)");

	/**
	 * split class into the class name and it's param
	 * 
	 * @param cl the class
	 * @return a list with the class and it's param (String => [String], List<String> => [List, String], Map<String,List<String>> => [Map, String, List<String>])
	 */
	public static List<String> parse(String cl) {
		Matcher m = CLAZZ.matcher(cl);
		if (!m.matches())
			throw new RuntimeException("malformed class " + cl);
		if (m.group(2) == null)
			return Arrays.asList(m.group(1));
		if (m.group(2).trim().isEmpty())
			return Arrays.asList(m.group(1), "");

		List<String> list = new ArrayList<>();
		list.add(m.group(1));
		m = CLAZZ_LIST.matcher(m.group(2));
		while (m.find())
			list.add(m.group(1));
		return list;
	}
}
