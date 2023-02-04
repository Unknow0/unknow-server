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
	 * @return declared methods
	 */
	Collection<MethodModel> methods();

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
}