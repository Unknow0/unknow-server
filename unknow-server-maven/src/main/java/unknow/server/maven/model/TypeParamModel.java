/**
 * 
 */
package unknow.server.maven.model;

import java.util.List;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithName;
import unknow.server.maven.model.util.WithParent;
import unknow.server.maven.model.util.WithType;

/**
 * @author unknow
 */
public interface TypeParamModel extends WithAnnotation, WithType, WithName, WithParent<ClassModel> {
	/**
	 * @return parameter bounds
	 */
	List<ClassModel> bounds();
}
