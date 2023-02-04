/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;

/**
 * @author unknow
 */
public class JvmClass implements ClassModel, JvmMod {
	private final ModelLoader loader;
	private final Class<?> cl;
	private ClassModel superType;
	private List<ClassModel> interfaces;
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
	public String toString() {
		return cl.toString();
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(cl.getAnnotations()).map(a -> new JvmAnnotation(a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public ClassModel superType() {
		if (Object.class == cl || cl.isInterface())
			return null;
		if (superType == null)
			superType = loader.get(cl.getGenericSuperclass().getTypeName()).asClass();
		return superType;
	}

	@Override
	public List<ClassModel> interfaces() {
		if (interfaces == null) {
			Type[] t = cl.getGenericInterfaces();
			interfaces = new ArrayList<>(t.length);
			for (int i = 0; i < t.length; i++)
				interfaces.add(loader.get(t[i].getTypeName()).asClass());
		}
		return interfaces;
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
	public int mod() {
		return cl.getModifiers();
	}
}
