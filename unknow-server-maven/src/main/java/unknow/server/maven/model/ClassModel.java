/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;
import java.util.List;

import unknow.server.maven.model.util.AncestrorIterable;
import unknow.server.maven.model.util.WithMod;

/**
 * @author unknow
 */
public interface ClassModel extends TypeModel, WithMod {
	/**
	 * @return super class name
	 */
	String superName();

	/**
	 * @return super type
	 */
	ClassModel superType();

	/**
	 * @return implemented interface
	 */
	List<ClassModel> interfaces();

	/**
	 * @return all the declared field
	 */
	Collection<FieldModel> fields();

	/**
	 * @param name field to get
	 * @return the declared field or null if not found
	 */
	default FieldModel field(String name) {
		for (FieldModel f : fields()) {
			if (name.equals(f.name()))
				return f;
		}
		return null;
	}

	/**
	 * @return declared methods
	 */
	Collection<MethodModel> methods();

	/**
	 * @return generic param
	 */
	List<TypeParamModel> parameters();

	/**
	 * @param i index of the parameter to get
	 * @return get the i'th parameter
	 */
	default TypeParamModel parameter(int i) {
		return parameters().get(i);
	}

	/**
	 * @see unknow.server.maven.model.util.AncestrorIterator
	 * @return ancestors
	 */
	default Iterable<ClassModel> ancestor() {
		return new AncestrorIterable(this);
	}

	@Override
	default boolean isAssignableFrom(TypeModel t) {
		if (!t.isClass())
			return false;

		for (ClassModel p : t.asClass().ancestor()) {
			if (p.name().equals(name()))
				return true;
		}
		return false;
	}

	/**
	 * @return the full name with the parameters
	 */
	@Override
	String toString();
}