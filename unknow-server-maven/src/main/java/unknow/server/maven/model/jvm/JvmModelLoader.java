/**
 * 
 */
package unknow.server.maven.model.jvm;

import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class JvmModelLoader extends ModelLoader {
	/** global jvm loader */
	public static final JvmModelLoader GLOBAL = new JvmModelLoader(JvmModelLoader.class.getClassLoader());

	private final ClassLoader cl;

	/**
	 * create new JvmModelLoader
	 * 
	 * @param cl
	 */
	public JvmModelLoader(ClassLoader cl) {
		this.cl = cl;
	}

	@Override
	protected TypeModel load(ModelLoader loader, String cl, TypeModel[] params) {
		Class<?> c = tryLoad(cl);
		if (c == null)
			return null;

		if (c.isEnum())
			return new JvmEnum(this, c, params);
		return new JvmClass(this, c, params);
	}

	private Class<?> tryLoad(String clazz) {
		while (true) {
			try {
				return cl.loadClass(clazz);
			} catch (@SuppressWarnings("unused") ClassNotFoundException e) {
				int i = clazz.lastIndexOf('.');
				if (i < 0)
					return null;
				clazz = clazz.substring(0, i) + "$" + clazz.substring(i + 1);
			}
		}
	}
}
