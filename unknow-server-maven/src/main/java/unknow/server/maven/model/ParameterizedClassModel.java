/**
 * 
 */
package unknow.server.maven.model;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author unknow
 */
public class ParameterizedClassModel implements ClassModel {
	private final ModelLoader loader;
	private final List<String> cl;
	private final ClassModel base;

	private List<TypeModel> params;

	/**
	 * create new ParametrizedModel
	 * 
	 * @param loader
	 * @param cl
	 */
	public ParameterizedClassModel(ModelLoader loader, List<String> cl) {
		this.loader = loader;
		this.cl = cl;
		this.base = loader.get(cl.get(0)).asClass();
	}

	public List<TypeModel> parameters() {
		if (params == null)
			params = cl.stream().map(c -> loader.get(c)).collect(Collectors.toList());
		return params;
	}

	public TypeModel parameter(int i) {
		return parameters().get(i);
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		return base.annotations();
	}

	@Override
	public String name() {
		return base.name();
	}

	@Override
	public String internalName() {
		return base.internalName();
	}

	@Override
	public ClassModel superType() {
		return base.superType();
	}

	@Override
	public List<ClassModel> interfaces() {
		return base.interfaces();
	}

	@Override
	public Collection<FieldModel> fields() {
		return base.fields();
	}

	@Override
	public Collection<MethodModel> methods() {
		return base.methods();
	}

	public TypeModel base() {
		return base;
	}

	@Override
	public boolean isTransient() {
		return base.isTransient();
	}

	@Override
	public boolean isStatic() {
		return base.isStatic();
	}

	@Override
	public boolean isPublic() {
		return base.isPublic();
	}

	@Override
	public boolean isProtected() {
		return base.isProtected();
	}

	@Override
	public boolean isPrivate() {
		return base.isPrimitive();
	}

	@Override
	public boolean isAbstract() {
		return base.isAbstract();
	}

	@Override
	public boolean isAssignableFrom(TypeModel t) {
		return base.isAssignableFrom(t);
	}
}
