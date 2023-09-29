/**
 * 
 */
package unknow.server.maven.model;

import unknow.server.maven.model.util.WithAnnotation;

/**
 * @author unknow
 */
public interface PackageModel extends WithAnnotation {
	/** @return the package name */
	String name();
}