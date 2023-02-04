/**
 * 
 */
package unknow.server.maven.model.ast;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.EnumModel;

/**
 * @author unknow
 */
public class AstEnum implements EnumModel {
	private final EnumDeclaration e;
	private List<AnnotationModel> annotations;
	private List<EnumConstant> entries;

	/**
	 * create new AstEnum
	 * 
	 * @param e
	 */
	public AstEnum(EnumDeclaration e) {
		this.e = e;
	}

	@Override
	public String name() {
		return e.resolve().getQualifiedName();
	}

	@Override
	public String toString() {
		return name();
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = e.getAnnotations().stream().map(a -> new AstAnnotation(a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public List<EnumConstant> entries() {
		if (entries == null)
			entries = e.getEntries().stream().map(c -> new AstEnumConstant(c)).collect(Collectors.toList());
		return entries;
	}

	private static class AstEnumConstant implements EnumConstant {
		private final EnumConstantDeclaration e;
		private List<AnnotationModel> annotations;

		/**
		 * create new AstEnumConstant
		 * 
		 * @param e
		 */
		public AstEnumConstant(EnumConstantDeclaration e) {
			this.e = e;
		}

		@Override
		public Collection<AnnotationModel> annotations() {
			if (annotations == null)
				annotations = e.getAnnotations().stream().map(a -> new AstAnnotation(a)).collect(Collectors.toList());
			return annotations;
		}

		@Override
		public String name() {
			return e.getNameAsString();
		}
	}
}
