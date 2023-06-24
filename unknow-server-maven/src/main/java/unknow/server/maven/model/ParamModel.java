/**
 * 
 */
package unknow.server.maven.model;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithName;
import unknow.server.maven.model.util.WithParent;
import unknow.server.maven.model.util.WithType;

/**
 * @author unknow
 * @param <T> owner model
 */
public interface ParamModel<T extends WithParent<ClassModel>> extends WithAnnotation, WithType, WithName, WithParent<T> { //ok
}
