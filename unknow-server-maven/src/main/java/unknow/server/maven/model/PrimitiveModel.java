/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;
import java.util.Collections;

/**
 * @author unknow
 */
public class PrimitiveModel implements TypeModel {
	public static final PrimitiveModel BOOLEAN = new PrimitiveModel("boolean", "Z", Boolean.class.getName());
	public static final PrimitiveModel BYTE = new PrimitiveModel("byte", "B", Byte.class.getName());
	public static final PrimitiveModel CHAR = new PrimitiveModel("char", "C", Character.class.getName());
	public static final PrimitiveModel SHORT = new PrimitiveModel("short", "S", Short.class.getName());
	public static final PrimitiveModel INT = new PrimitiveModel("int", "I", Integer.class.getName());
	public static final PrimitiveModel LONG = new PrimitiveModel("long", "L", Long.class.getName());
	public static final PrimitiveModel FLOAT = new PrimitiveModel("float", "F", Float.class.getName());
	public static final PrimitiveModel DOUBLE = new PrimitiveModel("double", "D", Double.class.getName());

	private final String name;
	private final String internal;
	private final String boxed;

	private PrimitiveModel(String name, String internal, String boxed) {
		this.name = name;
		this.internal = internal;
		this.boxed = boxed;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String internalName() {
		return internal;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		return Collections.emptyList();
	}

	@Override
	public boolean isAssignableFrom(TypeModel t) {
		return this == t;
	}

	/**
	 * @return the boxed type
	 */
	public String boxed() {
		return boxed;
	}
}
