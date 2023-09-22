/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.Map;

import com.github.javaparser.ast.body.TypeDeclaration;

import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class AstModelLoader extends ModelLoader {

	private final Map<String, TypeDeclaration<?>> classes;

	public AstModelLoader(Map<String, TypeDeclaration<?>> classes) {
		this.classes = classes;
	}

	@Override
	protected TypeModel load(ModelLoader loader, String cl, TypeModel[] params) {
		TypeDeclaration<?> t = classes.get(cl);
		if (t == null)
			return null;
		if (t.isEnumDeclaration())
			return new AstEnum(loader, t.asEnumDeclaration());
		else if (t.isClassOrInterfaceDeclaration())
			return new AstClass(loader, t.asClassOrInterfaceDeclaration(), params);
		throw new RuntimeException("unsuported type " + t);
	}
}
