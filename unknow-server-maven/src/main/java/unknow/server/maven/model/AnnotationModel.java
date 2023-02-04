/**
 * 
 */
package unknow.server.maven.model;

import java.util.Optional;

import unknow.server.maven.model.util.WithName;

/**
 * @author unknow
 */
public interface AnnotationModel extends WithName {

	/**
	 * @param name value to get
	 * @return the value as String
	 */
	Optional<String> value(String name);

	/**
	 * @return "value"
	 */
	default Optional<String> value() {
		return value("value");
	}

	/**
	 * @param name value to get
	 * @return the value as String array
	 */
	Optional<String[]> values(String name);

	/**
	 * @return "value"
	 */
	default Optional<String[]> values() {
		return values("value");
	}
}
