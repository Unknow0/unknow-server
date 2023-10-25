package unknow.server.maven.model;

import java.util.Arrays;
import java.util.function.Function;

import unknow.server.maven.model.EnumModel.EnumConstant;

public interface AnnotationValue {

	/** @return value as an array */
	AnnotationValue[] asArray();

	/** @return value as class */
	TypeModel asClass();

	/** @return value as an enum */
	EnumConstant asEnum();

	/**
	 * @return primitive, string and enum as string
	 */
	String asLiteral();

	/** @return value as annotation */
	AnnotationModel asAnnotation();

	default boolean asBoolean() {
		return Boolean.parseBoolean(asLiteral());
	}

	default int asInt() {
		return Integer.parseInt(asLiteral());
	}

	default <T> T[] asArray(T[] t, Function<AnnotationValue, T> f) {
		AnnotationValue[] a = asArray();
		if (a == null)
			return null;
		t = Arrays.copyOf(t, a.length);
		for (int i = 0; i < a.length; i++)
			t[i] = f.apply(a[i]);
		return t;
	}

	static final String[] STRING = {};
	static final boolean[] BOOLEAN = {};
	static final int[] INT = {};
	static final AnnotationModel[] ANNOT = {};

	default String[] asArrayLiteral() {
		return asArray(STRING, a -> a.asLiteral());
	}

	default boolean[] asArrayBoolean() {
		AnnotationValue[] a = asArray();
		if (a == null)
			return null;
		boolean[] t = new boolean[a.length];
		for (int i = 0; i < a.length; i++)
			t[i] = a[i].asBoolean();
		return t;
	}

	default int[] asArrayInt() {
		AnnotationValue[] a = asArray();
		if (a == null)
			return null;
		int[] t = new int[a.length];
		for (int i = 0; i < a.length; i++)
			t[i] = a[i].asInt();
		return t;
	}

	default AnnotationModel[] asArrayAnnotation() {
		return asArray(ANNOT, a -> a.asAnnotation());
	}

	public static final AnnotationValue NULL = new AnnotationValueNull();

	static class AnnotationValueNull implements AnnotationValue {
		private AnnotationValueNull() {
		}

		@Override
		public AnnotationValue[] asArray() {
			return this == NULL ? new AnnotationValue[0] : new AnnotationValue[] { this };
		}

		@Override
		public EnumConstant asEnum() {
			return null;
		}

		@Override
		public TypeModel asClass() {
			return null;
		}

		@Override
		public String asLiteral() {
			return null;
		}

		@Override
		public AnnotationModel asAnnotation() {
			return null;
		}
	}

	public static class AnnotationValueArray extends AnnotationValueNull {
		private final AnnotationValue[] a;

		public AnnotationValueArray(AnnotationValue[] a) {
			this.a = a;
		}

		@Override
		public AnnotationValue[] asArray() {
			return a;
		}
	}

	public static class AnnotationValueClass extends AnnotationValueNull {
		private final TypeModel c;

		public AnnotationValueClass(TypeModel c) {
			this.c = c;
		}

		@Override
		public TypeModel asClass() {
			return c;
		}

		@Override
		public String asLiteral() {
			return c.name();
		}
	}

	public static class AnnotationValueLiteral extends AnnotationValueNull {
		private final String s;

		public AnnotationValueLiteral(String s) {
			this.s = s;
		}

		@Override
		public String asLiteral() {
			return s;
		}
	}

	public static class AnnotationValueAnnotation extends AnnotationValueNull {
		private final AnnotationModel a;

		public AnnotationValueAnnotation(AnnotationModel a) {
			this.a = a;
		}

		@Override
		public String asLiteral() {
			return a.name();
		}

		@Override
		public AnnotationModel asAnnotation() {
			return a;
		}
	}

	public static class AnnotationValueEnum extends AnnotationValueNull {
		private final EnumConstant e;

		public AnnotationValueEnum(EnumConstant e) {
			this.e = e;
		}

		@Override
		public String asLiteral() {
			return e.name();
		}

		@Override
		public EnumConstant asEnum() {
			return e;
		}
	}
}