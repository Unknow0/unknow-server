/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;
import java.util.Optional;

import unknow.server.maven.model.util.WithName;

/**
 * @author unknow
 */
public interface AnnotationModel extends WithName {

	/**
	 * @return all annotation members
	 */
	Collection<AnnotationMemberModel> members();

	/**
	 * @return the value member
	 */
	default Optional<AnnotationMemberModel> value() {
		return member("value");
	}

	/**
	 * @param name
	 * @return the member with name
	 */
	default Optional<AnnotationMemberModel> member(String name) {
		return members().stream().filter(m -> name.equals(m.name())).findAny();
	}
}
