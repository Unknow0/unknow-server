/**
 * 
 */
package unknow.server.maven.model;

import java.util.List;

import unknow.server.maven.model.util.WithAnnotation;

/**
 * @author unknow
 */
public interface EnumModel extends TypeModel {

	@Override
	default boolean isEnum() {
		return true;
	}

	/**
	 * @return declared enum entries
	 */
	List<EnumConstant> entries();

	/**
	 * an enum constant
	 * 
	 * @author unknow
	 */
	public interface EnumConstant extends WithAnnotation {
		/**
		 * @return enum constant name
		 */
		String name();
	}
}
