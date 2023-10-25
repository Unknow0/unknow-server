/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ConstructorModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;

/**
 * @author unknow
 */
public class JvmConstructor implements ConstructorModel, JvmMod {
	private final ClassModel parent;
	private final ModelLoader loader;
	private final Constructor<?> c;
	private Collection<AnnotationModel> annotations;
	private List<ParamModel<ConstructorModel>> params;

	/**
	 * create new JvmField
	 * 
	 * @param parent the owner
	 * @param loader the loader
	 * @param c      the constructor
	 */
	public JvmConstructor(ClassModel parent, ModelLoader loader, Constructor<?> c) {
		this.parent = parent;
		this.loader = loader;
		this.c = c;
	}

	@Override
	public ClassModel parent() {
		return parent;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(c.getAnnotations()).map(a -> new JvmAnnotation(loader, a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public int mod() {
		return c.getModifiers();
	}

	@Override
	public List<ParamModel<ConstructorModel>> parameters() {
		if (params == null)
			params = Arrays.stream(c.getParameters()).map(p -> new JvmParam<ConstructorModel>(loader, this, p)).collect(Collectors.toList());
		return params;
	}

	@Override
	public String toString() {
		return c.toString();
	}
}