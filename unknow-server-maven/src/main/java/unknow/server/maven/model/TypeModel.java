/**
 * 
 */
package unknow.server.maven.model;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithName;
import unknow.server.maven.model.util.WithParent;

/**
 * @author unknow
 */
public interface TypeModel extends WithAnnotation, WithName, WithParent<PackageModel> {
	/**
	 * @return type fully qualified name with actual parameter
	 */
	@Override
	String name();

	/** @return generic name */
	default String genericName() {
		return name();
	}

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

	@Override
	default PackageModel parent() {
		return null;
	}

	default String packageName() {
		return parent() == null ? "" : parent().name();
	}

	/**
	 * @return true if it's a primitive type
	 */
	default boolean isPrimitive() {
		return this instanceof PrimitiveModel;
	}

	/**
	 * @return this as a primive
	 */
	default PrimitiveModel asPrimitive() {
		if (this instanceof PrimitiveModel)
			return (PrimitiveModel) this;
		throw new RuntimeException(name() + " isn't a primitive");
	}

	/**
	 * @return true if it's a class type
	 */
	default boolean isClass() {
		return this instanceof ClassModel;
	}

	/**
	 * @return this as a class
	 */
	default ClassModel asClass() {
		if (this instanceof ClassModel)
			return (ClassModel) this;
		throw new RuntimeException(name() + " isn't a class");
	}

	/**
	 * Determines if the type represented by this {@code TypeModel} object is either the same as, or is a superclass or superinterface of, the type represented by the specified {@code TypeModel} parameter
	 * 
	 * @param t the clazz
	 * @return true if clazz this = t works
	 */
	default boolean isAssignableFrom(TypeModel t) {
		return t.name().equals(name());
	}

	/**
	 * Determines if the type represented by the specified parameter is either the same as, or is a superclass or superinterface of, the type represented by this {@code TypeModel} object
	 * 
	 * @param cl the clazz
	 * @return true if cl = this works
	 */
	default boolean isAssignableTo(Class<?> cl) {
		return isAssignableTo(cl.getName());
	}

	/**
	 * Determines if the type represented by the specified parameter is either the same as, or is a superclass or superinterface of, the type represented by this {@code TypeModel} object
	 * 
	 * @param cl the clazz
	 * @return true if cl = this works
	 */
	default boolean isAssignableTo(String cl) {
		return cl.equals(name());
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
		throw new RuntimeException(name() + " isn't an array");
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
		throw new RuntimeException(name() + " isn't an enum");
	}

	/**
	 * @return true if it's a wildcard
	 */
	default boolean isWildCard() {
		return this instanceof WildcardModel;
	}

	/**
	 * @return this as a wildcard
	 */
	default WildcardModel asWildcard() {
		if (this instanceof WildcardModel)
			return (WildcardModel) this;
		throw new RuntimeException(name() + " isn't a wildcard");
	}

	/**
	 * @return true if it's void
	 */
	default boolean isVoid() {
		return false;
	}

	/**
	 * @param t
	 * @return true if type are equals
	 */
	default boolean equals(TypeModel t) {
		return t.toString().equals(toString());
	}

}