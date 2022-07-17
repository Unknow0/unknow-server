/**
 * 
 */
package unknow.server.maven.model.util;

import java.util.Collection;

import unknow.server.maven.model.AnnotationModel;

/**
 * @author unknow
 */
public interface WithAnnotation {
	/**
	 * @return the annotations
	 */
	Collection<AnnotationModel> annotations();

	/**
	 * @param cl annotation to get
	 * @return the annotation or null if missing
	 */
	default AnnotationModel annotation(Class<?> cl) {
		return annotation(cl.getName());
	}

	/**
	 * @param name fqn of the annotation
	 * @return the annotation or null if missing
	 */
	default AnnotationModel annotation(String name) {
		return annotations().stream().filter(a -> name.equals(a.name())).findFirst().orElse(null);
	}
}
