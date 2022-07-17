/**
 * 
 */
package unknow.server.maven.model.impl;

import java.util.Collection;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.Parameter;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class AstParam implements ParamModel {
	private final ModelLoader loader;
	private final Parameter p;
	private Collection<AnnotationModel> annotations;
	private TypeModel type;

	/**
	 * create new AstParam
	 * 
	 * @param loader
	 * @param p
	 */
	public AstParam(ModelLoader loader, Parameter p) {
		this.loader = loader;
		this.p = p;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null) {
			annotations = p.getAnnotations().stream().map(a -> new AstAnnotation(a)).collect(Collectors.toList());
		}
		return annotations;
	}

	@Override
	public String name() {
		return p.getNameAsString();
	}

	@Override
	public TypeModel type() {
		if (type == null)
			type = loader.get(p.getType().resolve().describe());
		return type;
	}

}
