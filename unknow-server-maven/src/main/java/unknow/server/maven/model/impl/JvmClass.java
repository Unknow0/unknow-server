/**
 * 
 */
package unknow.server.maven.model.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class JvmClass implements ClassModel {
	private final ModelLoader loader;
	private final Class<?> cl;
	private TypeModel superType;
	private Collection<AnnotationModel> annotations;
	private Collection<FieldModel> fields;
	private Collection<MethodModel> methods;

	public JvmClass(ModelLoader loader, Class<?> cl) {
		this.loader = loader;
		this.cl = cl;
	}

	@Override
	public String name() {
		return cl.getName();
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(cl.getAnnotations()).map(a -> new JvmAnnotation(a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public TypeModel superType() {
		if (superType == null)
			superType = loader.get(cl.getGenericSuperclass().getTypeName());
		return superType;
	}

	@Override
	public Collection<FieldModel> fields() {
		if (fields == null)
			fields = Arrays.stream(cl.getDeclaredFields()).map(f -> new JvmField(loader, f)).collect(Collectors.toList());
		return fields;
	}

	@Override
	public Collection<MethodModel> methods() {
		if (methods == null)
			methods = Arrays.stream(cl.getDeclaredMethods()).map(m -> new JvmMethod(loader, m)).collect(Collectors.toList());
		return methods;
	}
}
