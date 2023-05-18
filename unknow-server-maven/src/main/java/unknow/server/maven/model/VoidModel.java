/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;
import java.util.Collections;

/**
 * @author unknow
 */
public class VoidModel implements TypeModel {
	public static final TypeModel SELF = new VoidModel();

	private VoidModel() {
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		return Collections.emptyList();
	}

	@Override
	public String name() {
		return "void";
	}

	@Override
	public String internalName() {
		return "V";
	}

	@Override
	public boolean isVoid() {
		return true;
	}

	@Override
	public boolean isAssignableFrom(TypeModel t) {
		return t.isVoid();
	}
	
	@Override
	public String toString() {
		return name();
	}
}
