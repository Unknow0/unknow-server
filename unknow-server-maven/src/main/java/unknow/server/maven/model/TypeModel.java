/**
 * 
 */
package unknow.server.maven.model;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithName;

/**
 * @author unknow
 */
public interface TypeModel extends WithAnnotation, WithName {
	/**
	 * @return type fully qualified name
	 */
	@Override
	String name();

	/**
	 * @return java internalName
	 */
	default String internalName() {
		return "L" + name() + ";";
	}

	/**
	 * @return simpleName
	 */
	default String simpleName() {
		String name = name();
		int i = name.lastIndexOf(".");
		return i < 0 ? name : name.substring(i + 1);
	}

	/**
	 * @return package
	 */
	default String packageName() {
		String name = name();
		int i = name.lastIndexOf(".");
		return i < 0 ? "" : name.substring(0, i);
	}

	/**
	 * @return true if it's a primitive type
	 */
	default boolean isPrimitive() {
		return this instanceof PrimitiveModel;
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
		return this instanceof ClassModel;
	}

	default ClassModel asClass() {
		if (this instanceof ClassModel)
			return (ClassModel) this;
		throw new RuntimeException();
	}

	/**
	 * Determines if the class or interface represented by this {@code TypeModel} object is either the same as, or is a superclass or superinterface of, the class or interface represented by the specified {@code TypeModel} parameter
	 * 
	 * @param t the clazz
	 * @return true is clazz c = this works
	 */
	default boolean isAssignableFrom(TypeModel t) {
		return t.name().equals(name());
	}

	/**
	 * @return true if it's an array
	 */
	default boolean isArray() {
		return this instanceof ArrayModel;
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
		return this instanceof EnumModel;
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