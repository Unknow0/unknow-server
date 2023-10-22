/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import unknow.server.maven.model.AnnotationMemberModel;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.AnnotationValue;
import unknow.server.maven.model.AnnotationValue.AnnotationValueAnnotation;
import unknow.server.maven.model.AnnotationValue.AnnotationValueArray;
import unknow.server.maven.model.AnnotationValue.AnnotationValueClass;
import unknow.server.maven.model.AnnotationValue.AnnotationValueLiteral;
import unknow.server.maven.model.ModelLoader;

/**
 * @author unknow
 */
public class JvmAnnotation implements AnnotationModel {
	private final Annotation a;
	private final Collection<AnnotationMemberModel> members;

	/**
	 * create new JvmAnnotation
	 * 
	 * @param loader
	 * @param a
	 */
	public JvmAnnotation(ModelLoader loader, Annotation a) {
		this.a = a;
		this.members = new ArrayList<>();

		Method[] methods = a.annotationType().getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			Method m = methods[i];
			AnnotationValue def = getValue(loader, m.getDefaultValue());
			try {
				AnnotationValue value = getValue(loader, m.invoke(a));

				members.add(new AnnotationMemberModel(m.getName(), value, def));
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	@Override
	public String name() {
		return a.annotationType().getName();
	}

	@Override
	public Collection<AnnotationMemberModel> members() {
		return members;
	}

	@Override
	public String toString() {
		return name();
	}

	/**
	 * @param loader
	 * @param o
	 * @return object as annotation value
	 */
	public static AnnotationValue getValue(ModelLoader loader, Object o) {
		if (o == null)
			return AnnotationValue.NULL;
		if (o instanceof boolean[]) {
			boolean[] t = (boolean[]) o;
			AnnotationValue[] a = new AnnotationValue[t.length];
			for (int i = 0; i < t.length; i++)
				a[i] = new AnnotationValueLiteral(Boolean.toString(t[i]));
			return new AnnotationValueArray(a);
		}
		if (o instanceof byte[]) {
			byte[] t = (byte[]) o;
			AnnotationValue[] a = new AnnotationValue[t.length];
			for (int i = 0; i < t.length; i++)
				a[i] = new AnnotationValueLiteral(Byte.toString(t[i]));
			return new AnnotationValueArray(a);
		}
		if (o instanceof char[]) {
			char[] t = (char[]) o;
			AnnotationValue[] a = new AnnotationValue[t.length];
			for (int i = 0; i < t.length; i++)
				a[i] = new AnnotationValueLiteral(Character.toString(t[i]));
			return new AnnotationValueArray(a);
		}
		if (o instanceof short[]) {
			short[] t = (short[]) o;
			AnnotationValue[] a = new AnnotationValue[t.length];
			for (int i = 0; i < t.length; i++)
				a[i] = new AnnotationValueLiteral(Short.toString(t[i]));
			return new AnnotationValueArray(a);
		}
		if (o instanceof int[]) {
			int[] t = (int[]) o;
			AnnotationValue[] a = new AnnotationValue[t.length];
			for (int i = 0; i < t.length; i++)
				a[i] = new AnnotationValueLiteral(Integer.toString(t[i]));
			return new AnnotationValueArray(a);
		}
		if (o instanceof long[]) {
			long[] t = (long[]) o;
			AnnotationValue[] a = new AnnotationValue[t.length];
			for (int i = 0; i < t.length; i++)
				a[i] = new AnnotationValueLiteral(Long.toString(t[i]));
			return new AnnotationValueArray(a);
		}
		if (o instanceof float[]) {
			float[] t = (float[]) o;
			AnnotationValue[] a = new AnnotationValue[t.length];
			for (int i = 0; i < t.length; i++)
				a[i] = new AnnotationValueLiteral(Float.toString(t[i]));
			return new AnnotationValueArray(a);
		}
		if (o instanceof double[]) {
			double[] t = (double[]) o;
			AnnotationValue[] a = new AnnotationValue[t.length];
			for (int i = 0; i < t.length; i++)
				a[i] = new AnnotationValueLiteral(Double.toString(t[i]));
			return new AnnotationValueArray(a);
		}
		if (o.getClass().isArray()) {
			Object[] t = (Object[]) o;
			AnnotationValue[] a = new AnnotationValue[t.length];
			for (int i = 0; i < t.length; i++)
				a[i] = getValue(loader, t[i]);
			return new AnnotationValueArray(a);
		}
		if (o instanceof Class)
			return new AnnotationValueClass(loader.get(((Class<?>) o).getName()));
		if (o instanceof Enum)
			return new AnnotationValueLiteral(((Enum<?>) o).name());
		if (o instanceof Annotation)
			return new AnnotationValueAnnotation(new JvmAnnotation(loader, (Annotation) o));
		return new AnnotationValueLiteral(o.toString());
	}
}
