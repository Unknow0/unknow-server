/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;
import java.util.Collections;

import unknow.server.maven.model.util.WithType;

/**
 * @author unknow
 */
public class ArrayModel implements TypeModel, WithType {
	private final String name;
	private final TypeModel type;

	public ArrayModel(String name, TypeModel type) {
		this.name = name;
		this.type = type;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String internalName() {
		return "[" + type().internalName();
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		return Collections.emptyList();
	}

	/**
	 * @return component type
	 */
	@Override
	public TypeModel type() {
		return type;
	}

	@Override
	public boolean isAssignableFrom(TypeModel t) {
		return t.isArray() && type.isAssignableFrom(t.asArray().type);
	}

	@Override
	public String toString() {
		return name();
	}
}
