/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.util.WithParent;

public class JvmParam<T extends WithParent<ClassModel>> implements ParamModel<T> {
	private final ModelLoader loader;
	private final T m;
	private final Parameter p;
	private TypeModel type;
	private Collection<AnnotationModel> annotations;

	public JvmParam(ModelLoader loader, T m, Parameter p) {
		this.loader = loader;
		this.m = m;
		this.p = p;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(p.getAnnotations()).map(a -> new JvmAnnotation(loader, a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public String name() {
		return p.getName();
	}

	@Override
	public T parent() {
		return m;
	}

	@Override
	public TypeModel type() {
		if (type == null)
			type = loader.get(p.getParameterizedType().getTypeName(), m.parent().parameters());
		return type;
	}

	@Override
	public String toString() {
		return p.toString();
	}
}