/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class JvmMethod implements MethodModel {
	private final ClassModel parent;
	private final ModelLoader loader;
	private final Method m;
	private Collection<AnnotationModel> annotations;
	private List<ParamModel> params;

	/**
	 * create new JvmField
	 * 
	 * @param loader the loader
	 * @param m      the method
	 */
	public JvmMethod(ClassModel parent, ModelLoader loader, Method m) {
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
	public boolean isTransient() {
		return Modifier.isTransient(m.getModifiers());
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(m.getModifiers());
	}

	@Override
	public boolean isPublic() {
		return Modifier.isPublic(m.getModifiers());
	}

	@Override
	public boolean isProtected() {
		return Modifier.isProtected(m.getModifiers());
	}

	@Override
	public boolean isPrivate() {
		return Modifier.isPrivate(m.getModifiers());
	}

	@Override
	public boolean isAbstract() {
		return Modifier.isAbstract(m.getModifiers());
	}

	@Override
	public String name() {
		return m.getName();
	}

	@Override
	public String toString() {
		return m.toString();
	}

	@Override
	public TypeModel type() {
		return loader.get(m.getGenericReturnType().getTypeName());
	}

	@Override
	public List<ParamModel> parameters() {
		if (params == null)
			params = Arrays.stream(m.getParameters()).map(p -> new JvmParam(loader, this, p)).collect(Collectors.toList());
		return params;
	}
}