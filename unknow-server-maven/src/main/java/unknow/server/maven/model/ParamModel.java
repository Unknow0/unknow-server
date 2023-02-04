/**
 * 
 */
package unknow.server.maven.model;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithName;
import unknow.server.maven.model.util.WithType;

/**
 * @author unknow
 */
public interface ParamModel extends WithAnnotation, WithType, WithName {
	/**
	 * @return the method owning this parameter
	 */
	MethodModel method();
}
