/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.AnnotationValue;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class AstAnnotationMethod implements MethodModel, AstMod {
	private final ClassModel parent;
	private final ModelLoader loader;
	private final AnnotationMemberDeclaration m;
	private Collection<AnnotationModel> annotations;
	private TypeModel type;
	private AnnotationValue defaultValue;

	/**
	 * create new AstMethod
	 * 
	 * @param loader
	 * @param m
	 */
	public AstAnnotationMethod(ClassModel parent, ModelLoader loader, AnnotationMemberDeclaration m) {
		this.parent = parent;
		this.loader = loader;
		this.m = m;
	}

	@Override
	public ClassModel parent() {
		return parent;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null) {
			annotations = m.getAnnotations().stream().map(a -> new AstAnnotation(loader, a)).collect(Collectors.toList());
		}
		return annotations;
	}

	@Override
	public NodeWithModifiers<?> object() {
		return m;
	}

	@Override
	public String name() {
		return m.getNameAsString();
	}

	@Override
	public String toString() {
		return parent.name() + "." + signature();
	}

	@Override
	public TypeModel type() {
		if (type == null)
			type = loader.get(m.getType().resolve().describe(), parent.parameters());
		return type;
	}

	@Override
	public List<ParamModel<MethodModel>> parameters() {
		return Collections.emptyList();
	}

	@Override
	public AnnotationValue defaultValue() {
		if (defaultValue == null)
			defaultValue = m.getDefaultValue().map(v -> AstAnnotation.value(loader, v)).orElse(AnnotationValue.NULL);
		return defaultValue;
	}
}
