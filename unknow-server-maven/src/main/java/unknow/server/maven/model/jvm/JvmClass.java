/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ConstructorModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.TypeParamModel;

/**
 * @author unknow
 */
public class JvmClass implements ClassModel, JvmMod {
	protected final JvmModelLoader loader;
	protected final Class<?> cl;
	private final TypeModel[] paramsClass;
	private ClassModel superType;
	private List<ClassModel> interfaces;
	private Collection<ConstructorModel> constructors;
	private Collection<AnnotationModel> annotations;
	private Collection<FieldModel> fields;
	private Collection<MethodModel> methods;
	private List<TypeParamModel> parameters;

	public JvmClass(JvmModelLoader loader, Class<?> cl, TypeModel[] paramsClass) {
		this.loader = loader;
		this.cl = cl;
		this.paramsClass = paramsClass;
	}

	@Override
	public String name() {
		return cl.getTypeName();
	}

	@Override
	public String genericName() {
		return toString();
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(cl.getAnnotations()).map(a -> new JvmAnnotation(loader, a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public ClassModel superType() {
		if (cl.getGenericSuperclass() == null)
			return null;
		if (superType == null)
			superType = loader.get(cl.getGenericSuperclass().getTypeName(), parameters()).asClass();
		return superType;
	}

	@Override
	public List<ClassModel> interfaces() {
		if (interfaces == null) {
			Type[] t = cl.getGenericInterfaces();
			interfaces = new ArrayList<>(t.length);
			for (int i = 0; i < t.length; i++)
				interfaces.add(loader.get(t[i].getTypeName(), parameters()).asClass());
		}
		return interfaces;
	}

	@Override
	public Collection<ConstructorModel> constructors() {
		if (constructors == null)
			constructors = Arrays.stream(cl.getDeclaredConstructors()).map(c -> new JvmConstructor(this, loader, c)).collect(Collectors.toList());
		return constructors;
	}

	@Override
	public Collection<FieldModel> fields() {
		if (fields == null)
			fields = Arrays.stream(cl.getDeclaredFields()).map(f -> new JvmField(loader, this, f)).collect(Collectors.toList());
		return fields;
	}

	@Override
	public Collection<MethodModel> methods() {
		if (methods == null)
			methods = Arrays.stream(cl.getDeclaredMethods()).map(m -> new JvmMethod(this, loader, m)).collect(Collectors.toList());
		return methods;
	}

	@Override
	public List<TypeParamModel> parameters() {
		if (parameters == null) {
			TypeVariable<?>[] typeParameters = cl.getTypeParameters();
			parameters = new ArrayList<>(typeParameters.length);
			for (int i = 0; i < typeParameters.length; i++)
				parameters.add(new JvmTypeParam(loader, this, typeParameters[i], paramsClass.length == typeParameters.length ? paramsClass[i] : null));
		}
		return parameters;
	}

	@Override
	public int mod() {
		return cl.getModifiers();
	}

	@Override
	public String toString() {
		if (parameters().isEmpty())
			return cl.getTypeName();
		StringBuilder sb = new StringBuilder(cl.getTypeName()).append('<');
		for (TypeParamModel p : parameters())
			sb.append(p.type()).append(',');
		sb.setCharAt(sb.length() - 1, '>');
		return sb.toString();
	}
}
