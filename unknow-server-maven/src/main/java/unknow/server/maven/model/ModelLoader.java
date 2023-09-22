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

/**
 * @author unknow
 */
public abstract class ModelLoader {
	private static final Map<String, TypeModel> BUILTIN = new HashMap<>();
	private static final TypeModel[] EMPTY = {};

	protected static final Pattern CLAZZ = Pattern.compile("(.+?)(?:<(.*?)>)?");
	protected static final Pattern CLAZZ_LIST = Pattern.compile("(.+?(?:<.*?>)?)(?:,|$)");

	static {
		for (TypeModel t : PrimitiveModel.PRIMITIVES)
			BUILTIN.put(t.name(), t);
	}

	private final Map<String, TypeModel> cache = new HashMap<>();

	protected ModelLoader() {
	}

	public static ModelLoader from(ModelLoader... loaders) {
		if (loaders.length == 1)
			return loaders[1];
		return new CompositeLoader(loaders);
	}

	public TypeModel get(String cl) {
		return get(cl, Collections.emptyList());
	}

	public TypeModel get(String cl, List<TypeParamModel> parameters) {
		String key = cl + "#" + parameters;
		TypeModel t = BUILTIN.get(cl);
		if (t != null)
			return t;
		t = cache.get(key);
		if (t != null)
			return t;

		t = create(cl, parameters);
		if (t == null)
			throw new RuntimeException(this.getClass().getName() + ": Type not found " + cl);
		cache.put(key, t);
		return t;
	}

	private final TypeModel create(String cl, List<TypeParamModel> parameters) {
		if (cl.endsWith("[]"))
			return new ArrayModel(cl, get(cl.substring(0, cl.length() - 2), parameters));
		if (cl.equals("?"))
			return WildcardModel.EMPTY;
		if (cl.startsWith("? extends "))
			return new WildcardModel(get(cl.substring(10)), true);
		if (cl.startsWith("? super "))
			return new WildcardModel(get(cl.substring(8)), false);

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
		TypeModel t = map.get(cl);
		if (t != null)
			cl = t.name();

		return load(this, cl, params);
	}

	/**
	 * @param params
	 * @param cl
	 */
	protected abstract TypeModel load(ModelLoader loader, String cl, TypeModel[] params);

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
			return Arrays.asList(m.group(1).trim());
		if (m.group(2).trim().isEmpty())
			return Arrays.asList(m.group(1).trim(), "");

		List<String> list = new ArrayList<>();
		list.add(m.group(1));
		m = CLAZZ_LIST.matcher(m.group(2).trim());
		while (m.find())
			list.add(m.group(1).trim());
		return list;
	}

	private static class CompositeLoader extends ModelLoader {

		private final ModelLoader[] loaders;

		public CompositeLoader(ModelLoader... loaders) {
			this.loaders = loaders;
		}

		@Override
		protected TypeModel load(ModelLoader loader, String cl, TypeModel[] params) {
			for (int i = 0; i < loaders.length; i++) {
				TypeModel t = loaders[i].load(loader, cl, params);
				if (t != null)
					return t;
			}
			return null;
		}
	}
}
