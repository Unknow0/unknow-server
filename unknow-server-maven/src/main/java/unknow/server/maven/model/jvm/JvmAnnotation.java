/**
 * 
 */
package unknow.server.maven.model.jvm;

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
				return toString(m.invoke(a));
			} catch (Exception e) {
			}
			return null;
		};
		TOARRAY = m -> {
			try {
				m.setAccessible(true);
				Object o = m.invoke(a);
				if (o == null)
					return null;
				if (!o.getClass().isArray())
					return new String[] { toString(o) };
				Object[] t = (Object[]) o;
				String[] s = new String[t.length];
				for (int i = 0; i < t.length; i++)
					s[i] = toString(t[i]);
				return s;
			} catch (Exception e) {
			}
			return null;
		};
	}

	private static String toString(Object o) {
		if (o instanceof Class)
			return ((Class<?>) o).getName();
		if (o instanceof Enum)
			return ((Enum<?>) o).name();
		if (o != null)
			return String.valueOf(o);
		return null;
	}

	@Override
	public String name() {
		return clazz.getName();
	}

	@Override
	public String toString() {
		return clazz.toString();
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
