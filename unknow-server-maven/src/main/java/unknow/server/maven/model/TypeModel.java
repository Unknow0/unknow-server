/**
 * 
 */
package unknow.server.maven.model;

import unknow.server.maven.model.util.WithAnnotation;

/**
 * @author unknow
 */
public interface TypeModel extends WithAnnotation {
	/**
	 * @return type fully qualified name
	 */
	String name();

	/**
	 * @return simpleName
	 */
	default String simpleName() {
		String name = name();
		int i = name.lastIndexOf(".");
		return i < 0 ? name : name.substring(i);
	}

	/**
	 * @return true if it's a primitive type
	 */
	default boolean isPrimitive() {
		return false;
	}

	default PrimitiveModel asPrimitive() {
		if (this instanceof PrimitiveModel)
			return (PrimitiveModel) this;
		throw new RuntimeException();
	}

	/**
	 * @return true if it's a class type
	 */
	default boolean isClass() {
		return false;
	}

	default ClassModel asClass() {
		if (this instanceof ClassModel)
			return (ClassModel) this;
		throw new RuntimeException();
	}

	/**
	 * @return true if it's an array
	 */
	default boolean isArray() {
		return false;
	}

	/**
	 * @return this type as an array or null
	 */
	default ArrayModel asArray() {
		if (this instanceof ArrayModel)
			return (ArrayModel) this;
		throw new RuntimeException();
	}

	/**
	 * @return true if it's an enum
	 */
	default boolean isEnum() {
		return false;
	}

	/**
	 * @return this type as an enum or null
	 */
	default EnumModel asEnum() {
		if (this instanceof EnumModel)
			return (EnumModel) this;
		throw new RuntimeException();
	}

	/**
	 * @return true if it's a void
	 */
	default boolean isVoid() {
		return false;
	}
}