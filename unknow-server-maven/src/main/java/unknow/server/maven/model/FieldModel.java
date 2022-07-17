/**
 * 
 */
package unknow.server.maven.model;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithMod;

/**
 * @author unknow
 */
public interface FieldModel extends WithAnnotation,WithMod {
	/**
	 * @return field name
	 */
	String name();

	/**
	 * @return field type
	 */
	TypeModel type();

}
