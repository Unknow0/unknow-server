package unknow.server.maven.model.ast;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ConstructorModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeParamModel;

public class AstAnnotationDecl implements ClassModel, AstMod {
	private final ModelLoader loader;
	private final AnnotationDeclaration a;
	private String name;
	private Collection<AnnotationModel> annotations;
	private Collection<MethodModel> methods;

	protected AstAnnotationDecl(ModelLoader loader, AnnotationDeclaration a) {
		this.loader = loader;
		this.a = a;
	}

	@Override
	public ClassModel superType() {
		return loader.get(Object.class.getName()).asClass();
	}

	@Override
	public List<ClassModel> interfaces() {
		return Collections.emptyList();
	}

	@Override
	public List<TypeParamModel> parameters() {
		return Collections.emptyList();
	}

	@Override
	public String name() {
		if (name == null) {
			name = a.getNameAsString();
			Node p = a.getParentNode().orElse(null);
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
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = a.getAnnotations().stream().map(a -> new AstAnnotation(loader, a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public NodeWithModifiers<?> object() {
		return a;
	}

	@Override
	public Collection<ConstructorModel> constructors() {
		return Collections.emptyList();
	}

	@Override
	public Collection<FieldModel> fields() {
		return Collections.emptyList();
	}

	@Override
	public Collection<MethodModel> methods() {
		if (methods == null)
			methods = a.getMembers().stream().filter(m -> m instanceof AnnotationMemberDeclaration)
					.map(m -> new AstAnnotationMethod(this, loader, (AnnotationMemberDeclaration) m)).collect(Collectors.toList());
		return methods;
	}

	@Override
	public String toString() {
		return "@" + name();
	}
}
