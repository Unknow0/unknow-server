/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.Collection;
import java.util.stream.Collectors;

import com.github.javaparser.ast.PackageDeclaration;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.PackageModel;

/**
 * @author unknow
 */
public class AstPackage implements PackageModel {
	private final ModelLoader loader;
	private final PackageDeclaration p;
	private Collection<AnnotationModel> annotations;

	/**
	 * create new AstPackage
	 * 
	 * @param loader
	 * @param p
	 */
	public AstPackage(ModelLoader loader, PackageDeclaration p) {
		this.loader = loader;
		this.p = p;
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = p.getAnnotations().stream().map(a -> new AstAnnotation(loader, a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public String name() {
		return p.getNameAsString();
	}

}
