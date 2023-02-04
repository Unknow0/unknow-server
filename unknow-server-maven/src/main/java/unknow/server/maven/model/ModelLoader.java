/**
 * 
 */
package unknow.server.maven.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
	private static final Collection<TypeModel> BUILTIN = Arrays.asList(VoidModel.SELF, PrimitiveModel.BOOLEAN, PrimitiveModel.BYTE, PrimitiveModel.CHAR, PrimitiveModel.SHORT, PrimitiveModel.INT, PrimitiveModel.LONG, PrimitiveModel.FLOAT, PrimitiveModel.DOUBLE);

	private final Map<String, TypeDeclaration<?>> classes;
	private final Map<String, TypeModel> models;

	public ModelLoader(Map<String, TypeDeclaration<?>> classes) {
		this.classes = classes;
		this.models = new HashMap<>();
		for (TypeModel t : BUILTIN)
			this.models.put(t.name(), t);
	}

	public TypeModel get(String cl) {
		TypeModel m = models.get(cl);
		if (m == null)
			models.put(cl, m = createModel(cl));
		return m;
	}

	@SuppressWarnings("unchecked")
	private TypeModel createModel(String cl) {
		if (cl.endsWith("[]"))
			return new ArrayModel(this, cl);
		List<String> parse = parse(cl);
		if (parse.size() > 1)
			return new ParameterizedClassModel(this, parse);

		TypeDeclaration<?> t = classes.get(cl);
		if (t != null) {
			if (t.isEnumDeclaration())
				return new AstEnum(t.asEnumDeclaration());
			else if (t.isClassOrInterfaceDeclaration())
				return new AstClass(this, t.asClassOrInterfaceDeclaration());
			throw new RuntimeException("unsuported type " + t);
		}
		try {
			Class<?> c = Class.forName(cl);
			if (c.isEnum())
				return new JvmEnum((Class<? extends Enum<?>>) c);
			return new JvmClass(this, c);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	protected static final Pattern CLAZZ = Pattern.compile("(.+?)(?:<(.*?)>)?");
	protected static final Pattern CLAZZ_LIST = Pattern.compile("(.+?(?:<.*?>)?)(?:,|$)");

	public static List<String> parse(String cl) {
		Matcher m = CLAZZ.matcher(cl);
		if (!m.matches())
			throw new RuntimeException("malformed class " + cl);
		if (m.group(2) == null)
			return Arrays.asList(m.group(1));

		List<String> list = new ArrayList<>();
		list.add(m.group(1));
		m = CLAZZ_LIST.matcher(m.group(2));
		while (m.find())
			list.add(m.group(1));
		return list;
	}
}
