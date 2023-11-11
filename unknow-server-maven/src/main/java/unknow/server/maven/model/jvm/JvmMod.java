/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.reflect.Modifier;

import unknow.server.maven.model.util.WithMod;

/**
 * @author unknow
 */
public interface JvmMod extends WithMod {
	/**
	 * @return modifier
	 */
	int mod();

	@Override
	default boolean isTransient() {
		return Modifier.isTransient(mod());
	}

	@Override
	default boolean isStatic() {
		return Modifier.isStatic(mod());
	}

	@Override
	default boolean isPublic() {
		return Modifier.isPublic(mod());
	}

	@Override
	default boolean isProtected() {
		return Modifier.isProtected(mod());
	}

	@Override
	default boolean isPrivate() {
		return Modifier.isPrivate(mod());
	}

	@Override
	default boolean isAbstract() {
		return Modifier.isAbstract(mod());
	}

}
