/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class JvmField implements FieldModel, JvmMod {
	private final JvmModelLoader loader;
	private final ClassModel cl;
	private final Field f;
	private Collection<AnnotationModel> annotations;

	/**
	 * create new JvmField
	 * 
	 * @param loader the loader
	 * @param cl
	 * @param f      the field
	 */
	public JvmField(JvmModelLoader loader, ClassModel cl, Field f) {
		this.loader = loader;
		this.cl = cl;
		this.f = f;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(f.getAnnotations()).map(a -> new JvmAnnotation(loader, a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public int mod() {
		return f.getModifiers();
	}

	@Override
	public String name() {
		return f.getName();
	}

	@Override
	public String toString() {
		return f.toString();
	}

	@Override
	public ClassModel parent() {
		return cl;
	}

	@Override
	public TypeModel type() {
		return loader.get(f.getGenericType().getTypeName(), cl.parameters());
	}
}