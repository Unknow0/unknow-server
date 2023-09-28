/**
 * 
 */
package unknow.server.maven.model;

import java.util.List;
import java.util.Optional;

import unknow.server.maven.model.util.WithAnnotation;

/**
 * @author unknow
 */
public interface EnumModel extends ClassModel {

	@Override
	default boolean isEnum() {
		return true;
	}

	/**
	 * @return declared enum entries
	 */
	List<EnumConstant> entries();

	default Optional<EnumConstant> entry(String name) {
		return entries().stream().filter(e -> e.name().equals(name)).findAny();
	}

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
