/**
 * 
 */
package unknow.server.maven.model.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class JvmField implements FieldModel {
	private final ModelLoader loader;
	private final Field f;
	private Collection<AnnotationModel> annotations;

	/**
	 * create new JvmField
	 * 
	 * @param loader the loader
	 * @param f      the field
	 */
	public JvmField(ModelLoader loader, Field f) {
		this.loader = loader;
		this.f = f;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(f.getAnnotations()).map(a -> new JvmAnnotation(a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public boolean isTransient() {
		return Modifier.isTransient(f.getModifiers());
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(f.getModifiers());
	}

	@Override
	public boolean isPublic() {
		return Modifier.isPublic(f.getModifiers());
	}

	@Override
	public boolean isProtected() {
		return Modifier.isProtected(f.getModifiers());
	}

	@Override
	public boolean isPrivate() {
		return Modifier.isPrivate(f.getModifiers());
	}

	@Override
	public String name() {
		return f.getName();
	}

	@Override
	public TypeModel type() {
		return loader.get(f.getGenericType().getTypeName());
	}
}