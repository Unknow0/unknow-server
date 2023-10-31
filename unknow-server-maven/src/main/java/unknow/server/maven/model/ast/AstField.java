/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class AstField implements FieldModel, AstMod<FieldDeclaration> {
	private final ModelLoader loader;
	private final ClassModel cl;
	private final FieldDeclaration f;
	private final VariableDeclarator v;
	private List<AnnotationModel> annotations;
	private TypeModel type;

	/**
	 * create new AstField
	 * 
	 * @param loader
	 * @param cl
	 * @param f
	 * @param v
	 */
	public AstField(ModelLoader loader, ClassModel cl, FieldDeclaration f, VariableDeclarator v) {
		this.loader = loader;
		this.cl = cl;
		this.f = f;
		this.v = v;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = f.getAnnotations().stream().map(a -> new AstAnnotation(loader, a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public FieldDeclaration object() {
		return f;
	}

	@Override
	public String name() {
		return v.getNameAsString();
	}

	@Override
	public ClassModel parent() {
		return cl;
	}

	@Override
	public String toString() {
		return type() + " " + name();
	}

	@Override
	public TypeModel type() {
		if (type == null)
			type = loader.get(v.getType().resolve().describe(), cl.parameters());
		return type;
	}

}
