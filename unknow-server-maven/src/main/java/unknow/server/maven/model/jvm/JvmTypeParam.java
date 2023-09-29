/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.TypeParamModel;

/**
 * @author unknow
 */
public class JvmTypeParam implements TypeParamModel {
	private final JvmModelLoader loader;
	private final ClassModel c;
	private final TypeVariable<?> t;
	private final TypeModel type;
	private Collection<AnnotationModel> annotations;
	private List<ClassModel> bounds;

	/**
	 * create new JvmTypeParam
	 * 
	 * @param loader
	 * @param c
	 * @param t
	 * @param type
	 */
	public JvmTypeParam(JvmModelLoader loader, ClassModel c, TypeVariable<?> t, TypeModel type) {
		this.loader = loader;
		this.c = c;
		this.t = t;
		this.type = type;
	}

	@Override
	public String name() {
		return t.getName();
	}

	@Override
	public ClassModel parent() {
		return c;
	}

	@Override
	public TypeModel type() {
		return type;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(t.getAnnotations()).map(a -> new JvmAnnotation(loader, a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public List<ClassModel> bounds() {
		if (bounds == null)
			bounds = Arrays.stream(t.getBounds()).map(b -> loader.get(b.getTypeName(), c.parameters()).asClass()).collect(Collectors.toList());
		return bounds;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(name());
		if (type() != null)
			sb.append('[').append(type()).append(']');
		return sb.toString();
	}

}