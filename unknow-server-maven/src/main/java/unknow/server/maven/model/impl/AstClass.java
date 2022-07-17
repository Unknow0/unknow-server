/**
 * 
 */
package unknow.server.maven.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class AstClass implements ClassModel {
	private final ModelLoader loader;
	private final ClassOrInterfaceDeclaration cl;
	private final String name;
	private TypeModel superType;
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
	public Collection<AnnotationModel> annotations() {
		if (annotations == null) {
			annotations = cl.getAnnotations().stream().map(a -> new AstAnnotation(a)).collect(Collectors.toList());
		}
		return annotations;
	}

	@Override
	public TypeModel superType() {
		if (superType == null)
			superType = loader.get(cl.resolve().asClass().getSuperClass().map(c -> c.describe()).orElse("java.lang.Object"));
		return superType;
	}

	@Override
	public Collection<FieldModel> fields() {
		if (fields == null) {
			fields = new ArrayList<>();
			for (FieldDeclaration f : cl.getFields())
				f.getVariables().stream().map(v -> new AstField(loader, f, v)).forEach(fields::add);
		}
		return fields;
	}

	@Override
	public Collection<MethodModel> methods() {
		if (methods == null)
			methods = cl.getMethods().stream().map(m -> new AstMethod(loader, m)).collect(Collectors.toList());
		return methods;
	}
}
