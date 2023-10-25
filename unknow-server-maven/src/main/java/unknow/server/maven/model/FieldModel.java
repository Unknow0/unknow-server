/**
 * 
 */
package unknow.server.maven.model;

import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithMod;
import unknow.server.maven.model.util.WithName;
import unknow.server.maven.model.util.WithParent;
import unknow.server.maven.model.util.WithType;

/**
 * @author unknow
 */
public interface FieldModel extends WithAnnotation, WithType, WithMod, WithName, WithParent<ClassModel> { //ok
}
