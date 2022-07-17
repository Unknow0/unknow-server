/**
 * 
 */
package unknow.server.maven.model.impl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class AstMethod implements MethodModel {
	private final ModelLoader loader;
	private final MethodDeclaration m;
	private Collection<AnnotationModel> annotations;
	private TypeModel type;
	private List<ParamModel> params;

	/**
	 * create new AstMethod
	 * 
	 * @param loader
	 * @param m
	 */
	public AstMethod(ModelLoader loader, MethodDeclaration m) {
		this.loader = loader;
		this.m = m;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null) {
			annotations = m.getAnnotations().stream().map(a -> new AstAnnotation(a)).collect(Collectors.toList());
		}
		return annotations;
	}

	@Override
	public boolean isTransient() {
		return m.getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.TRANSIENT);
	}

	@Override
	public boolean isStatic() {
		return m.getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.STATIC);
	}

	@Override
	public boolean isPublic() {
		return m.getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PUBLIC);
	}

	@Override
	public boolean isProtected() {
		return m.getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PROTECTED);
	}

	@Override
	public boolean isPrivate() {
		return m.getModifiers().stream().anyMatch(m -> m.getKeyword() == Modifier.Keyword.PRIVATE);
	}

	@Override
	public String name() {
		return m.getNameAsString();
	}

	@Override
	public TypeModel type() {
		if (type == null)
			type = loader.get(m.getType().resolve().describe());
		return type;
	}

	@Override
	public List<ParamModel> parameters() {
		if (params == null)
			params = m.getParameters().stream().map(p -> new AstParam(loader, p)).collect(Collectors.toList());
		return params;
	}

}
