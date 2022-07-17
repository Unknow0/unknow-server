/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;

/**
 * @author unknow
 */
public interface ClassModel extends TypeModel {

	/**
	 * @return super type
	 */
	TypeModel superType();

	/**
	 * @return all the declared field
	 */
	Collection<FieldModel> fields();

	/**
	 * @return declared methods
	 */
	Collection<MethodModel> methods();

	@Override
	default boolean isClass() {
		return true;
	}
}