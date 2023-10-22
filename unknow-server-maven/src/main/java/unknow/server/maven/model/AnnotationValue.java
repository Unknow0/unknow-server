package unknow.server.maven.model;

import java.util.Arrays;
import java.util.function.Function;

/**
 * @author unknow
 */
public interface AnnotationValue {

	/** @return value as an array */
	AnnotationValue[] asArray();

	/** @return value as class */
	TypeModel asClass();

	/**
	 * @return primitive, string and enum as string
	 */
	String asLiteral();

	/** @return value as annotation */
	AnnotationModel asAnnotation();

	/** @return value as boolean */
	default boolean asBoolean() {
		return Boolean.parseBoolean(asLiteral());
	}

	/** @return value as int */
	default int asInt() {
		return Integer.parseInt(asLiteral());
	}

	default <T extends Enum<T>> T asEnum(Class<T> cl) {
		return Enum.valueOf(cl, asLiteral());
	}

	/**
	 * @param <T> array component
	 * @param t   empty array for type
	 * @param f   conversion function
	 * @return value as an array
	 */
	default <T> T[] asArray(T[] t, Function<AnnotationValue, T> f) {
		AnnotationValue[] a = asArray();
		if (a == null)
			return t;
		t = Arrays.copyOf(t, a.length);
		for (int i = 0; i < a.length; i++)
			t[i] = f.apply(a[i]);
		return t;
	}

	/** empty string array */
	static final String[] STRING = {};
	/** empty boolean array */
	static final boolean[] BOOLEAN = {};
	/** empty int array */
	static final int[] INT = {};
	/** empty annotation array */
	static final AnnotationModel[] ANNOT = {};

	/** @return value as an array of literal */
	default String[] asArrayLiteral() {
		return asArray(STRING, a -> a.asLiteral());
	}

	/** @return value as an array of boolean */
	default boolean[] asArrayBoolean() {
		AnnotationValue[] a = asArray();
		if (a == null)
			return new boolean[0];
		boolean[] t = new boolean[a.length];
		for (int i = 0; i < a.length; i++)
			t[i] = a[i].asBoolean();
		return t;
	}

	/** @return value as an array of int */
	default int[] asArrayInt() {
		AnnotationValue[] a = asArray();
		if (a == null)
			return new int[0];
		int[] t = new int[a.length];
		for (int i = 0; i < a.length; i++)
			t[i] = a[i].asInt();
		return t;
	}

	/** @return value as an array of annotation */
	default AnnotationModel[] asArrayAnnotation() {
		return asArray(ANNOT, a -> a.asAnnotation());
	}

	/** null value */
	public static final AnnotationValue NULL = new AnnotationValueNull();

	/**
	 * @author unknow
	 */
	static class AnnotationValueNull implements AnnotationValue {
		private AnnotationValueNull() {
		}

		@Override
		public AnnotationValue[] asArray() {
			return this == NULL ? new AnnotationValue[0] : new AnnotationValue[] { this };
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

	/**
	 * @author unknow
	 */
	public static class AnnotationValueArray extends AnnotationValueNull {
		private final AnnotationValue[] a;

		/**
		 * create new AnnotationValueArray
		 * 
		 * @param a
		 */
		public AnnotationValueArray(AnnotationValue[] a) {
			this.a = a;
		}

		@Override
		public AnnotationValue[] asArray() {
			return a;
		}
	}

	/**
	 * @author unknow
	 */
	public static class AnnotationValueClass extends AnnotationValueNull {
		private final TypeModel c;

		/**
		 * create new AnnotationValueClass
		 * 
		 * @param c
		 */
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

	/**
	 * @author unknow
	 */
	public static class AnnotationValueLiteral extends AnnotationValueNull {
		private final String s;

		/**
		 * create new AnnotationValueLiteral
		 * 
		 * @param s
		 */
		public AnnotationValueLiteral(String s) {
			this.s = s;
		}

		@Override
		public String asLiteral() {
			return s;
		}
	}

	/**
	 * @author unknow
	 */
	public static class AnnotationValueAnnotation extends AnnotationValueNull {
		private final AnnotationModel a;

		/**
		 * create new AnnotationValueAnnotation
		 * 
		 * @param a
		 */
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
}