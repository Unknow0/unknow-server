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
	private final ModelLoader loader;
	private final String name;
	private TypeModel type;

	public ArrayModel(ModelLoader loader, String name) {
		this.loader = loader;
		this.name = name;
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
		if (type == null)
			type = loader.get(name.substring(0, name.length() - 2));
		return type;
	}

	@Override
	public boolean isAssignableFrom(TypeModel t) {
		return t.isArray() && type.isAssignableFrom(t.asArray().type);
	}
}
