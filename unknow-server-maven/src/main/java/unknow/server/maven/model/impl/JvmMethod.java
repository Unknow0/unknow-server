/**
 * 
 */
package unknow.server.maven.model.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class JvmMethod implements MethodModel {
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
	public JvmMethod(ModelLoader loader, Method m) {
		this.loader = loader;
		this.m = m;
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
	public String name() {
		return m.getName();
	}

	@Override
	public TypeModel type() {
		return loader.get(m.getGenericReturnType().getTypeName());
	}

	@Override
	public List<ParamModel> parameters() {
		if (params == null)
			params = Arrays.stream(m.getParameters()).map(p -> new JvmParam(loader, p)).collect(Collectors.toList());
		return params;
	}

	private static class JvmParam implements ParamModel {
		private final ModelLoader loader;
		private final Parameter p;
		private TypeModel type;
		private Collection<AnnotationModel> annotations;

		public JvmParam(ModelLoader loader, Parameter p) {
			this.loader = loader;
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
		public TypeModel type() {
			if (type == null)
				type = loader.get(p.getParameterizedType().getTypeName());
			return type;
		}
	}
}