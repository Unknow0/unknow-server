/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;

import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.TypeParamModel;

/**
 * @author unknow
 */
public class AstClass extends AstBaseClass<ClassOrInterfaceDeclaration> {
	private final TypeModel[] paramsClass;
	private ClassModel superType;
	private List<ClassModel> interfaces;
	private List<TypeParamModel> parameters;

	/**
	 * create new AstClass
	 * 
	 * @param loader the loader
	 * @param cl     the class declaration
	 */
	public AstClass(ModelLoader loader, ClassOrInterfaceDeclaration cl, TypeModel[] paramsClass) {
		super(loader, cl);
		this.paramsClass = paramsClass;
	}


	@Override
	public ClassModel superType() {
		if (c.isInterface())
			return null;
		if (superType == null) {
			ResolvedReferenceTypeDeclaration r = c.resolve();
			superType = loader.get(r.asClass().getSuperClass().map(c -> c.describe()).get(), parameters()).asClass();
		}
		return superType;
	}

	@Override
	public List<ClassModel> interfaces() {
		if (interfaces == null) {
			interfaces = (c.isInterface() ? c.getExtendedTypes() : c.getImplementedTypes()).stream().map(c -> loader.get(c.resolve().describe(), parameters()).asClass()).filter(c -> !"java.lang.Object".equals(c.name())).collect(Collectors.toList());
		}
		return interfaces;
	}

	@Override
	public List<TypeParamModel> parameters() {
		if (parameters == null) {
			int l = c.getTypeParameters().size();
			parameters = new ArrayList<>(l);
			for (int i = 0; i < l; i++)
				parameters.add(new AstTypeParam(loader, this, c.getTypeParameter(i), paramsClass.length == l ? paramsClass[i] : null));
		}
		return parameters;
	}
}
