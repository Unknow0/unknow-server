/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ConstructorModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.ParamModel;

/**
 * @author unknow
 */
public class AstConstructor implements ConstructorModel, AstMod {
	private final ClassModel parent;
	private final ModelLoader loader;
	private final ConstructorDeclaration c;
	private Collection<AnnotationModel> annotations;
	private List<ParamModel<ConstructorModel>> params;

	/**
	 * create new AstMethod
	 * 
	 * @param parent
	 * @param loader
	 * @param c
	 */
	public AstConstructor(ClassModel parent, ModelLoader loader, ConstructorDeclaration c) {
		this.parent = parent;
		this.loader = loader;
		this.c = c;
	}

	@Override
	public ClassModel parent() {
		return parent;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null) {
			annotations = c.getAnnotations().stream().map(a -> new AstAnnotation(loader, a)).collect(Collectors.toList());
		}
		return annotations;
	}

	@Override
	public NodeWithModifiers<? extends Node> object() {
		return c;
	}

	@Override
	public List<ParamModel<ConstructorModel>> parameters() {
		if (params == null) {
			int i = 0;
			params = new ArrayList<>();
			for (Parameter p : c.getParameters())
				params.add(new AstParam<>(loader, this, p, i++));
		}
		return params;
	}

	@Override
	public String toString() {
		return parent.name() + "." + signature();
	}
}
