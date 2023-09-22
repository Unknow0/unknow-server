/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class JvmMethod implements MethodModel, JvmMod {
	private final ClassModel parent;
	private final JvmModelLoader loader;
	private final Method m;
	private Collection<AnnotationModel> annotations;
	private List<ParamModel<MethodModel>> params;

	/**
	 * create new JvmField
	 * 
	 * @param loader the loader
	 * @param m      the method
	 */
	public JvmMethod(ClassModel parent, JvmModelLoader loader, Method m) {
		this.parent = parent;
		this.loader = loader;
		this.m = m;
	}

	@Override
	public ClassModel parent() {
		return parent;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(m.getAnnotations()).map(a -> new JvmAnnotation(a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public int mod() {
		return m.getModifiers();
	}

	@Override
	public String name() {
		return m.getName();
	}

	@Override
	public TypeModel type() {
		return loader.get(m.getGenericReturnType().getTypeName(), parent.parameters());
	}

	@SuppressWarnings("unused")
	@Override
	public List<ParamModel<MethodModel>> parameters() {
		if (params == null)
			params = Arrays.stream(m.getParameters()).map(p -> new JvmParam<MethodModel>(loader, this, p)).collect(Collectors.toList());
		return params;
	}

	@Override
	public String toString() {
		return m.toString();
	}
}