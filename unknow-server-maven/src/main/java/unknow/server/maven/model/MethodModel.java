/**
 * 
 */
package unknow.server.maven.model;

import java.util.List;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithMod;

/**
 * @author unknow
 */
public interface MethodModel extends WithAnnotation, WithMod {
	/**
	 * @return method name
	 */
	String name();

	/**
	 * @return the return type
	 */
	TypeModel type();

	/**
	 * @return method parameters
	 */
	List<ParamModel> parameters();
}
