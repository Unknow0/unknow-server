/**
 * 
 */
package unknow.server.maven.model.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import unknow.server.maven.model.AnnotationModel;

/**
 * @author unknow
 */
public class JvmAnnotation implements AnnotationModel {
	private final Class<? extends Annotation> clazz;

	private final Function<Method, String> TOSTRING;
	private final Function<Method, String[]> TOARRAY;

	public JvmAnnotation(Annotation a) {
		this.clazz = a.annotationType();
		this.TOSTRING = m -> {
			try {
				m.setAccessible(true);
				Object o = m.invoke(a);
				if (o instanceof Class)
					return ((Class<?>) o).getName();
				if (o != null)
					return String.valueOf(o);
			} catch (Exception e) {
			}
			return null;
		};
		TOARRAY = m -> {
			try {
				m.setAccessible(true);
				Object o = m.invoke(a);
				if (!o.getClass().isArray())
					return new String[] { String.valueOf(o) };
				Object[] t = (Object[]) o;
				String[] s = new String[t.length];
				for (int i = 0; i < t.length; i++)
					s[i] = String.valueOf(t[i]);
				return s;
			} catch (Exception e) {
			}
			return null;
		};

	}

	@Override
	public String name() {
		return clazz.getName();
	}

	@Override
	public Optional<String> value(String name) {
		return Arrays.stream(clazz.getDeclaredMethods()).filter(m -> name.equals(m.getName())).findFirst().map(TOSTRING);
	}

	@Override
	public Optional<String[]> values(String name) {
		return Arrays.stream(clazz.getDeclaredMethods()).filter(m -> name.equals(m.getName())).findFirst().map(TOARRAY);
	}
}
