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
import unknow.server.maven.model.AnnotationValue.AnnotationValueEnum;
import unknow.server.maven.model.AnnotationValue.AnnotationValueLiteral;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.jvm.JvmEnum.JvmEnumConstant;

/**
 * @author unknow
 */
public class JvmAnnotation implements AnnotationModel {
	private final Annotation a;
	private final Collection<AnnotationMemberModel> members;

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
				throw new RuntimeException(e);
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

	public static AnnotationValue getValue(ModelLoader loader, Object o) {
		if (o == null)
			return AnnotationValue.NULL;
		if (o.getClass().isArray()) {
			Class<?> cl = o.getClass().getComponentType();
			if (cl == boolean.class) {
				boolean[] t = (boolean[]) o;
				AnnotationValue[] a = new AnnotationValue[t.length];
				for (int i = 0; i < t.length; i++)
					a[i] = new AnnotationValueLiteral(Boolean.toString(t[i]));
				return new AnnotationValueArray(a);
			}
			if (cl == byte.class) {
				byte[] t = (byte[]) o;
				AnnotationValue[] a = new AnnotationValue[t.length];
				for (int i = 0; i < t.length; i++)
					a[i] = new AnnotationValueLiteral(Byte.toString(t[i]));
				return new AnnotationValueArray(a);
			}
			if (cl == char.class) {
				char[] t = (char[]) o;
				AnnotationValue[] a = new AnnotationValue[t.length];
				for (int i = 0; i < t.length; i++)
					a[i] = new AnnotationValueLiteral(Character.toString(t[i]));
				return new AnnotationValueArray(a);
			}
			if (cl == short.class) {
				short[] t = (short[]) o;
				AnnotationValue[] a = new AnnotationValue[t.length];
				for (int i = 0; i < t.length; i++)
					a[i] = new AnnotationValueLiteral(Short.toString(t[i]));
				return new AnnotationValueArray(a);
			}
			if (cl == int.class) {
				int[] t = (int[]) o;
				AnnotationValue[] a = new AnnotationValue[t.length];
				for (int i = 0; i < t.length; i++)
					a[i] = new AnnotationValueLiteral(Integer.toString(t[i]));
				return new AnnotationValueArray(a);
			}
			if (cl == long.class) {
				long[] t = (long[]) o;
				AnnotationValue[] a = new AnnotationValue[t.length];
				for (int i = 0; i < t.length; i++)
					a[i] = new AnnotationValueLiteral(Long.toString(t[i]));
				return new AnnotationValueArray(a);
			}
			if (cl == float.class) {
				float[] t = (float[]) o;
				AnnotationValue[] a = new AnnotationValue[t.length];
				for (int i = 0; i < t.length; i++)
					a[i] = new AnnotationValueLiteral(Float.toString(t[i]));
				return new AnnotationValueArray(a);
			}
			if (cl == double.class) {
				double[] t = (double[]) o;
				AnnotationValue[] a = new AnnotationValue[t.length];
				for (int i = 0; i < t.length; i++)
					a[i] = new AnnotationValueLiteral(Double.toString(t[i]));
				return new AnnotationValueArray(a);
			}
			Object[] t = (Object[]) o;
			AnnotationValue[] a = new AnnotationValue[t.length];
			for (int i = 0; i < t.length; i++)
				a[i] = getValue(loader, t[i]);
			return new AnnotationValueArray(a);
		}
		if (o instanceof Class)
			return new AnnotationValueClass(loader.get(((Class<?>) o).getName()));
		if (o instanceof Enum)
			return new AnnotationValueEnum(new JvmEnumConstant(loader, (Enum<?>) o));
		if (o instanceof Annotation)
			return new AnnotationValueAnnotation(new JvmAnnotation(loader, (Annotation) o));
		return new AnnotationValueLiteral(o.toString());
	}
}
