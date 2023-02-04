/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.ArrayList;
import java.util.Collection;
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

/**
 * @author unknow
 */
public class AstClass implements ClassModel, AstMod {
	private final ModelLoader loader;
	private final ClassOrInterfaceDeclaration cl;
	private final String name;
	private ClassModel superType;
	private List<ClassModel> interfaces;
	private Collection<AnnotationModel> annotations;
	private Collection<FieldModel> fields;
	private Collection<MethodModel> methods;

	public AstClass(ModelLoader loader, ClassOrInterfaceDeclaration cl) {
		this.loader = loader;
		this.cl = cl;
		this.name = cl.resolve().getQualifiedName();
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String toString() {
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
	public ClassModel superType() {
		if (superType == null) {
			ResolvedReferenceTypeDeclaration r = cl.resolve();
			if (r.isInterface())
				superType = loader.get("java.lang.Object").asClass();
			else
				superType = loader.get(r.asClass().getSuperClass().map(c -> c.describe()).orElse("java.lang.Object")).asClass();
		}
		return superType;
	}

	@Override
	public List<ClassModel> interfaces() {
		if (interfaces == null) {
			interfaces = (cl.isInterface() ? cl.getExtendedTypes() : cl.getImplementedTypes()).stream().map(c -> loader.get(c.resolve().describe()).asClass()).filter(c -> !"java.lang.Object".equals(c.name())).collect(Collectors.toList());
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
}
