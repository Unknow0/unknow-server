/**
 * 
 */
package unknow.server.maven.model;

import java.util.Optional;

/**
 * @author unknow
 */
public interface AnnotationModel {
	/**
	 * @return annotation class
	 */
	String name();

	/**
	 * @param name value to get
	 * @return the value as String
	 */
	Optional<String> value(String name);

	/**
	 * @param name value to get
	 * @return the value as String array
	 */
	Optional<String[]> values(String name);
}
