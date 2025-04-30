/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.Map;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Name;

import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class AstModelLoader extends ModelLoader {

	private final Map<String, TypeDeclaration<?>> classes;
	private final Map<String, PackageDeclaration> packages;

	/**
	 * create new AstModelLoader
	 * 
	 * @param classes the existing class
	 * @param packages the existing packages
	 */
	public AstModelLoader(Map<String, TypeDeclaration<?>> classes, Map<String, PackageDeclaration> packages) {
		this.classes = classes;
		this.packages = packages;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected TypeModel load(ModelLoader loader, String cl, TypeModel[] params) {
		TypeDeclaration<?> t = classes.get(cl);
		if (t == null)
			return null;
		String name = t.findAncestor(CompilationUnit.class).flatMap(cu -> cu.getPackageDeclaration()).map(v -> v.getNameAsString()).orElse(null);
		PackageDeclaration p = packages.get(name);
		if (p == null && name != null)
			p = new PackageDeclaration(new Name(name));
		if (t.isEnumDeclaration())
			return new AstEnum(loader, p, t.asEnumDeclaration());
		else if (t.isClassOrInterfaceDeclaration())
			return new AstClass(loader, p, t.asClassOrInterfaceDeclaration(), params);
		throw new IllegalArgumentException("unsuported type " + t);
	}
}
