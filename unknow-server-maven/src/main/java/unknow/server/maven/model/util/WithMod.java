/**
 * 
 */
package unknow.server.maven.model.util;

/**
 * @author unknow
 */
public interface WithMod {

	/** @return true is it's transient */
	boolean isTransient();

	/** @return true is it's static */
	boolean isStatic();

	/** @return true is it's public */
	boolean isPublic();

	/** @return true is it's protected */
	boolean isProtected();

	/** @return true is it's private */
	boolean isPrivate();

	/** @return true is it's abstract */
	boolean isAbstract();
}
