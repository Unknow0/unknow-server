/**
 * 
 */
package unknow.server.maven.model.jvm;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.EnumModel;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class JvmEnum extends JvmClass implements EnumModel {
	private List<AnnotationModel> annotations;
	private List<EnumConstant> entries;

	/**
	 * create new AstEnum
	 * 
	 * @param loader the loader
	 * @param cl the enum class
	 * @param params the generic params
	 */
	public JvmEnum(JvmModelLoader loader, Class<?> cl, TypeModel[] params) {
		super(loader, cl, params);
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(cl.getAnnotations()).map(a -> new JvmAnnotation(loader, a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public List<EnumConstant> entries() {
		if (entries == null)
			entries = Arrays.stream(cl.getEnumConstants()).map(c -> new JvmEnumConstant(loader, (Enum<?>) c)).collect(Collectors.toList());
		return entries;
	}

	/**
	 * @author unknow
	 */
	public static class JvmEnumConstant implements EnumConstant {
		private final JvmModelLoader loader;
		private final Enum<?> e;
		private List<AnnotationModel> annotations;

		/**
		 * create new AstEnumConstant
		 * 
		 * @param loader the loader
		 * @param e the enum entry
		 */
		public JvmEnumConstant(JvmModelLoader loader, Enum<?> e) {
			this.loader = loader;
			this.e = e;
		}

		@Override
		public Collection<AnnotationModel> annotations() {
			if (annotations == null) {
				try {
					annotations = Arrays.stream(e.getClass().getField(name()).getAnnotations()).map(a -> new JvmAnnotation(loader, a)).collect(Collectors.toList());
				} catch (NoSuchFieldException x) {
					throw new IllegalStateException(x);
				}
			}
			return annotations;
		}

		@Override
		public String name() {
			return e.name();
		}
	}
}
