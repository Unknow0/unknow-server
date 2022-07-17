/**
 * 
 */
package unknow.server.maven.model;

import unknow.server.maven.model.util.WithAnnotation;

/**
 * @author unknow
 */
public interface ParamModel extends WithAnnotation {
	/**
	 * @return parameter name
	 */
	String name();

	/**
	 * @return parameter type
	 */
	TypeModel type();
}
