/**
 * 
 */
package unknow.server.maven.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import unknow.server.maven.model.jvm.JvmModelLoader;

/**
 * @author unknow
 */
public class PrimitiveModel implements TypeModel {
	/** void type */
	public static final PrimitiveModel VOID = new PrimitiveModel("void", "Z", Void.class.getName()) {
		@Override
		public boolean isVoid() {
			return true;
		}
	};
	/** boolean type */
	public static final PrimitiveModel BOOLEAN = new PrimitiveModel("boolean", "Z", Boolean.class.getName());
	/** byte type */
	public static final PrimitiveModel BYTE = new PrimitiveModel("byte", "B", Byte.class.getName());
	/** char type */
	public static final PrimitiveModel CHAR = new PrimitiveModel("char", "C", Character.class.getName());
	/** short type */
	public static final PrimitiveModel SHORT = new PrimitiveModel("short", "S", Short.class.getName());
	/** int type */
	public static final PrimitiveModel INT = new PrimitiveModel("int", "I", Integer.class.getName());
	/** long type */
	public static final PrimitiveModel LONG = new PrimitiveModel("long", "L", Long.class.getName());
	/** float type */
	public static final PrimitiveModel FLOAT = new PrimitiveModel("float", "F", Float.class.getName());
	/** double type */
	public static final PrimitiveModel DOUBLE = new PrimitiveModel("double", "D", Double.class.getName());

	/** all primitives types */
	public static final List<PrimitiveModel> PRIMITIVES = Arrays.asList(VOID, BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE);

	private final String name;
	private final String binary;
	private final String boxed;

	private PrimitiveModel(String name, String binary, String boxed) {
		this.name = name;
		this.binary = binary;
		this.boxed = boxed;
	}

	@Override
	public String name() {
		return binary;
	}

	@Override
	public String simpleName() {
		return name;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		return Collections.emptyList();
	}

	@Override
	public boolean isAssignableFrom(TypeModel t) {
		return this == t || boxed.equals(t.name());
	}

	@Override
	public boolean isAssignableTo(String cl) {
		return name().equals(cl) || boxed.equals(cl);
	}

	/**
	 * @return the boxed type
	 */
	public TypeModel boxed() {
		return JvmModelLoader.GLOBAL.get(boxed);
	}

	@Override
	public String toString() {
		return name;
	}
}
