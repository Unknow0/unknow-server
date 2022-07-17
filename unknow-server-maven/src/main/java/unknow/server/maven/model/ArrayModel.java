/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;
import java.util.Collections;

/**
 * @author unknow
 */
public class ArrayModel implements TypeModel {
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
	public Collection<AnnotationModel> annotations() {
		return Collections.emptyList();
	}

	/**
	 * @return component type
	 */
	public TypeModel type() {
		if (type == null)
			type = loader.get(name.substring(0, name.length() - 2));
		return type;
	}

	@Override
	public boolean isArray() {
		return true;
	}
}
