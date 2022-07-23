/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.util.Collection;
import java.util.Collections;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class DummyModel implements TypeModel {

	private final String name;

	public DummyModel(String name) {
		this.name = name;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		return Collections.emptyList();
	}

	@Override
	public String name() {
		return name;
	}
}
