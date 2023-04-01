/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;

public class JvmParam implements ParamModel {
	private final ModelLoader loader;
	private final MethodModel m;
	private final Parameter p;
	private TypeModel type;
	private Collection<AnnotationModel> annotations;

	public JvmParam(ModelLoader loader, MethodModel m, Parameter p) {
		this.loader = loader;
		this.m = m;
		this.p = p;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(p.getAnnotations()).map(a -> new JvmAnnotation(a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public String name() {
		return p.getName();
	}

	@Override
	public MethodModel method() {
		return m;
	}

	@Override
	public TypeModel type() {
		if (type == null)
			type = loader.get(p.getParameterizedType().getTypeName());
		return type;
	}

	@Override
	public String toString() {
		return p.toString();
	}
}