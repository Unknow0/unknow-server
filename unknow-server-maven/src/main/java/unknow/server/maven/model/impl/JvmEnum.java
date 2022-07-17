/**
 * 
 */
package unknow.server.maven.model.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.EnumModel;

/**
 * @author unknow
 */
public class JvmEnum implements EnumModel {
	private final Class<? extends Enum<?>> cl;
	private List<AnnotationModel> annotations;
	private List<EnumConstant> entries;

	/**
	 * create new AstEnum
	 * 
	 * @param cl
	 */
	public JvmEnum(Class<? extends Enum<?>> cl) {
		this.cl = cl;
	}

	@Override
	public String name() {
		return cl.getName();
	}

	@Override
	public Collection<AnnotationModel> annotations() {
		if (annotations == null)
			annotations = Arrays.stream(cl.getAnnotations()).map(a -> new JvmAnnotation(a)).collect(Collectors.toList());
		return annotations;
	}

	@Override
	public List<EnumConstant> entries() {
		if (entries == null)
			entries = Arrays.stream(cl.getEnumConstants()).map(c -> new JvmEnumConstant(c)).collect(Collectors.toList());
		return entries;
	}

	private static class JvmEnumConstant implements EnumConstant {
		private final Enum<?> e;
		private List<AnnotationModel> annotations;

		/**
		 * create new AstEnumConstant
		 * 
		 * @param e
		 */
		public JvmEnumConstant(Enum<?> e) {
			this.e = e;
		}

		@Override
		public Collection<AnnotationModel> annotations() {
			if (annotations == null) {
				try {
					annotations = Arrays.stream(e.getClass().getField(name()).getAnnotations()).map(a -> new JvmAnnotation(a)).collect(Collectors.toList());
				} catch (NoSuchFieldException e) {
					throw new RuntimeException(e);
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
