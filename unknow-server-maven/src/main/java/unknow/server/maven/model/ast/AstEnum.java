/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.EnumModel;
import unknow.server.maven.model.ModelLoader;
import unknow.server.maven.model.TypeParamModel;

/**
 * @author unknow
 */
public class AstEnum extends AstBaseClass<EnumDeclaration> implements EnumModel {
	private List<EnumConstant> entries;
	private List<ClassModel> interfaces;

	/**
	 * create new AstEnum
	 * 
	 * @param loader the loader
	 * @param p the package
	 * @param e the enum
	 */
	public AstEnum(ModelLoader loader, PackageDeclaration p, EnumDeclaration e) {
		super(loader, p, e);
	}

	@Override
	public String name() {
		return c.resolve().getQualifiedName();
	}

	@Override
	public String toString() {
		return name();
	}

	@Override
	public List<EnumConstant> entries() {
		if (entries == null)
			entries = c.getEntries().stream().map(c -> new AstEnumConstant(loader, c)).collect(Collectors.toList());
		return entries;
	}

	@Override
	public ClassModel superType() {
		return loader.get("java.lang.Enum<" + name() + ">").asClass();
	}

	@Override
	public List<ClassModel> interfaces() {
		if (interfaces == null)
			interfaces = c.getImplementedTypes().stream().map(c -> loader.get(c.resolve().describe(), parameters()).asClass())
					.filter(c -> !"java.lang.Object".equals(c.name())).collect(Collectors.toList());
		return interfaces;
	}

	@Override
	public List<TypeParamModel> parameters() {
		return Collections.emptyList();
	}

	private static class AstEnumConstant implements EnumConstant {
		private final ModelLoader loader;
		private final EnumConstantDeclaration e;
		private List<AnnotationModel> annotations;

		/**
		 * create new AstEnumConstant
		 * 
		 * @param loader
		 * @param e
		 */
		public AstEnumConstant(ModelLoader loader, EnumConstantDeclaration e) {
			this.loader = loader;
			this.e = e;
		}

		@Override
		public Collection<AnnotationModel> annotations() {
			if (annotations == null)
				annotations = e.getAnnotations().stream().map(a -> new AstAnnotation(loader, a)).collect(Collectors.toList());
			return annotations;
		}

		@Override
		public String name() {
			return e.getNameAsString();
		}
	}

}
