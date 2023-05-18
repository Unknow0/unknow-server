/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.TypeParamModel;

/**
 * @author unknow
 */
public class AstClass implements ClassModel, AstMod {
	private final ModelLoader loader;
	private final ClassOrInterfaceDeclaration cl;
	private final String name;
	private final TypeModel[] paramsClass;
	private ClassModel superType;
	private List<ClassModel> interfaces;
	private Collection<AnnotationModel> annotations;
	private Collection<FieldModel> fields;
	private Collection<MethodModel> methods;
	private List<TypeParamModel> parameters;

	/**
	 * create new AstClass
	 * 
	 * @param loader the loader
	 * @param cl     the class declaration
	 */
	public AstClass(ModelLoader loader, ClassOrInterfaceDeclaration cl, TypeModel[] paramsClass) {
		this.loader = loader;
		this.cl = cl;
		this.name = cl.resolve().getQualifiedName();
		this.paramsClass = paramsClass;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public NodeWithModifiers<?> object() {
		return cl;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null) {
			annotations = cl.getAnnotations().stream().map(a -> new AstAnnotation(a)).collect(Collectors.toList());
		}
		return annotations;
	}

	@Override
	public String superName() {
		return cl.resolve().asClass().getSuperClass().map(c -> c.describe()).orElse(null);
	}

	@Override
	public ClassModel superType() {
		if (cl.isInterface())
			return null;
		if (superType == null) {
			ResolvedReferenceTypeDeclaration r = cl.resolve();
			superType = loader.get(r.asClass().getSuperClass().map(c -> c.describe()).get(), parameters()).asClass();
		}
		return superType;
	}

	@Override
	public List<ClassModel> interfaces() {
		if (interfaces == null) {
			interfaces = (cl.isInterface() ? cl.getExtendedTypes() : cl.getImplementedTypes()).stream().map(c -> loader.get(c.resolve().describe(), parameters()).asClass()).filter(c -> !"java.lang.Object".equals(c.name())).collect(Collectors.toList());
		}
		return interfaces;
	}

	@Override
	public Collection<FieldModel> fields() {
		if (fields == null) {
			fields = new ArrayList<>();
			for (FieldDeclaration f : cl.getFields())
				f.getVariables().stream().map(v -> new AstField(loader, this, f, v)).forEach(fields::add);
		}
		return fields;
	}

	@Override
	public Collection<MethodModel> methods() {
		if (methods == null)
			methods = cl.getMethods().stream().map(m -> new AstMethod(this, loader, m)).collect(Collectors.toList());
		return methods;
	}

	@Override
	public List<TypeParamModel> parameters() {
		if (parameters == null) {
			int l = cl.getTypeParameters().size();
			if (paramsClass.length == l) {
				parameters = new ArrayList<>(l);
				for (int i = 0; i < l; i++)
					parameters.add(new AstTypeParam(loader, this, cl.getTypeParameter(i), paramsClass[i]));
			} else
				parameters = Collections.emptyList();
		}
		return parameters;
	}

	@Override
	public String toString() {
		if (parameters().isEmpty())
			return name;
		StringBuilder sb = new StringBuilder(name).append('<');
		for (TypeParamModel p : parameters())
			sb.append(p.type()).append(',');
		sb.setCharAt(sb.length() - 1, '>');
		return sb.toString();

	}
}
