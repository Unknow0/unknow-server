/**
 * 
 */
package unknow.server.maven.model.util;

/**
 * @author unknow
 */
public interface WithMod {

	boolean isTransient();

	boolean isStatic();

	boolean isPublic();

	boolean isProtected();

	boolean isPrivate();
}
