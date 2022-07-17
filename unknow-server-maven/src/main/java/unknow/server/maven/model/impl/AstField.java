/**
 * 
 */
package unknow.server.maven.model.impl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class AstField implements FieldModel {
	private final ModelLoader loader;
	private final FieldDeclaration f;
	private final VariableDeclarator v;
	private List<AnnotationModel> annotations;
	private TypeModel type;

	/**
	 * create new AstField
	 * 
	 * @param loader
	 * @param f
	 * @param v
	 */
	public AstField(ModelLoader loader, FieldDeclaration f, VariableDeclarator v) {
		this.loader = loader;
		this.f = f;
		this.v = v;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = f.getAnnotations().stream().map(a -> new AstAnnotation(a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public boolean isTransient() {
		return f.getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.TRANSIENT);
	}

	@Override
	public boolean isStatic() {
		return f.getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.STATIC);
	}

	@Override
	public boolean isPublic() {
		return f.getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PUBLIC);
	}

	@Override
	public boolean isProtected() {
		return f.getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PROTECTED);
	}

	@Override
	public boolean isPrivate() {
		return f.getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PRIVATE);
	}

	@Override
	public String name() {
		return v.getNameAsString();
	}

	@Override
	public TypeModel type() {
		if (type == null)
			type = loader.get(v.getType().resolve().describe());
		return type;
	}

}
