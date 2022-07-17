/**
 * 
 */
package unknow.server.maven.model.impl;

import java.util.Collection;
import java.util.Collections;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.TypeModel;

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
	public boolean isVoid() {
		return true;
	}

}
