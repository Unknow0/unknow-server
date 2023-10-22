/**
 * 
 */
package unknow.server.maven.model.ast;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;

import unknow.server.maven.model.util.WithMod;

/**
 * @author unknow
 */
public interface AstMod<T extends NodeWithModifiers<?>> extends WithMod {
	/**
	 * @return the object with modifier
	 */
	T object();

	@Override
	default boolean isTransient() {
		return object().getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.TRANSIENT);
	}

	@Override
	default boolean isStatic() {
		return object().getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.STATIC);
	}

	@Override
	default boolean isPublic() {
		return object().getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PUBLIC);
	}

	@Override
	default boolean isProtected() {
		return object().getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PROTECTED);
	}

	@Override
	default boolean isPrivate() {
		return object().getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PRIVATE);
	}

	@Override
	default boolean isAbstract() {
		return object().getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.ABSTRACT);
	}
}
