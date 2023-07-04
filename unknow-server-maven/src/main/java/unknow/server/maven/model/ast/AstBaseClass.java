/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ConstructorModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeParamModel;

/**
 * @author unknow
 */
public abstract class AstBaseClass<T extends TypeDeclaration<?>> implements ClassModel, AstMod {
	protected final ModelLoader loader;
	protected final T c;
	private String name;
	private Collection<AnnotationModel> annotations;
	private Collection<ConstructorModel> constructors;
	private Collection<FieldModel> fields;
	private Collection<MethodModel> methods;

	protected AstBaseClass(ModelLoader loader, T c) {
		this.loader = loader;
		this.c = c;
	}

	@Override
	public String name() {
		if (name == null) {
			name = c.getNameAsString();
			Node p = c.getParentNode().orElse(null);
			while (p != null) {
				if (p instanceof ClassOrInterfaceDeclaration || p instanceof EnumDeclaration)
					name = ((NodeWithSimpleName<?>) p).getNameAsString() + "$" + name;
				if (p instanceof CompilationUnit) {
					Optional<PackageDeclaration> o = ((CompilationUnit) p).getPackageDeclaration();
					if (o.isPresent())
						name = o.get().getNameAsString() + "." + name;
				}
				p = p.getParentNode().orElse(null);
			}
		}
		return name;
	}

	@Override
	public String genericName() {
		if (parameters().isEmpty())
			return name();
		StringBuilder sb = new StringBuilder(name()).append('<');
		for (TypeParamModel p : parameters())
			sb.append(p.type()).append(',');
		sb.setCharAt(sb.length() - 1, '>');
		return sb.toString();
	}

	@Override
	public NodeWithModifiers<?> object() {
		return c;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null) {
			annotations = c.getAnnotations().stream().map(a -> new AstAnnotation(a)).collect(Collectors.toList());
		}
		return annotations;
	}

	@Override
	public Collection<ConstructorModel> constructors() {
		if (constructors == null)
			constructors = c.getConstructors().stream().map(c -> new AstConstructor(this, loader, c)).collect(Collectors.toList());
		return constructors;
	}

	@Override
	public Collection<FieldModel> fields() {
		if (fields == null) {
			fields = new ArrayList<>();
			for (FieldDeclaration f : c.getFields())
				f.getVariables().stream().map(v -> new AstField(loader, this, f, v)).forEach(fields::add);
		}
		return fields;
	}

	@Override
	public Collection<MethodModel> methods() {
		if (methods == null)
			methods = c.getMethods().stream().map(m -> new AstMethod(this, loader, m)).collect(Collectors.toList());
		return methods;
	}

	@Override
	public String toString() {
		return genericName();
	}
}
