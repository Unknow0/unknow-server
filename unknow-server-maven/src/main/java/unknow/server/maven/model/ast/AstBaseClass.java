/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ConstructorModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;

/**
 * @author unknow
 */
public abstract class AstBaseClass<T extends TypeDeclaration<?>> implements ClassModel, AstMod {
	protected final ModelLoader loader;
	protected final T c;
	private Collection<AnnotationModel> annotations;
	private Collection<ConstructorModel> constructors;
	private Collection<FieldModel> fields;
	private Collection<MethodModel> methods;

	protected AstBaseClass(ModelLoader loader, T c) {
		this.loader = loader;
		this.c = c;
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
}
