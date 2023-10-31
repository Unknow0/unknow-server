package unknow.server.maven.model.simple;

import java.util.Arrays;

import unknow.server.maven.model.AnnotationValue;
import unknow.server.maven.model.AnnotationValue.AnnotationValueNull;

public class SimpleAnnotationArray extends AnnotationValueNull {
	private AnnotationValue[] a;

	/**
		 * create new AnnotationValueArray
		 * 
		 * @param a
		 */
	public SimpleAnnotationArray() {
		this.a = new AnnotationValue[0];
	}

	public SimpleAnnotationArray with(AnnotationValue v) {
		a = Arrays.copyOf(a, a.length + 1);
		a[a.length - 1] = v;
		return this;
	}

	public SimpleAnnotationArray withNull() {
		return with(AnnotationValue.NULL);
	}

	public SimpleAnnotationArray withLiteral(String value) {
		return with(new AnnotationValueLiteral(value));
	}

	public SimpleAnnotation withAnnotation(Class<?> clazz) {
		return withAnnotation(clazz.getName());
	}

	private SimpleAnnotation withAnnotation(String name) {
		SimpleAnnotation a = new SimpleAnnotation(name);
		with(new AnnotationValueAnnotation(a));
		return a;
	}

	@Override
	public AnnotationValue[] asArray() {
		return a;
	}
}
