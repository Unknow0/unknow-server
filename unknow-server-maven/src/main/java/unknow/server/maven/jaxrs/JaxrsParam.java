/**
 * 
 */
package unknow.server.maven.jaxrs;

import java.util.Map;
import java.util.function.Consumer;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Encoded;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.ParamModel;
import unknow.server.maven.model.PrimitiveModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.util.WithAnnotation;
import unknow.server.maven.model.util.WithName;
import unknow.server.maven.model.util.WithType;

/**
 * @author unknow
 */
public abstract class JaxrsParam<T extends WithName & WithAnnotation & WithType> {
	public final T p;
	public final TypeModel type;
	public final TypeModel parent;
	public final String name;
	public final String value;
	public final String var;
	public final String def;
	public final boolean encoded;

	private JaxrsParam(T p, String prefix, String value) {
		this.p = p;
		if (p instanceof ParamModel)
			this.parent = ((ParamModel<?>) p).parent().parent();
		else if (p instanceof FieldModel)
			this.parent = ((FieldModel) p).parent();
		else
			throw new IllegalArgumentException("unsupported param type " + p);

		this.name = p.name();
		this.type = p.type();
		this.value = value;

		this.var = prefix + "$" + p.name();
		this.def = p.annotation(DefaultValue.class).flatMap(a -> a.value()).orElseGet(() -> {
			if (!type.isPrimitive())
				return null;
			if (type == PrimitiveModel.BOOLEAN)
				return "false";
			return "0";
		});
		this.encoded = p.annotation(Encoded.class).isPresent();
	}

	public void collect(Consumer<JaxrsParam<?>> c) {
		c.accept(this);
	}

	public static class JaxrsBeanParam<T extends WithName & WithAnnotation & WithType> extends JaxrsParam<T> {
		public final ClassModel clazz;
		public final Map<FieldModel, JaxrsParam<?>> fields;
		public final Map<MethodModel, JaxrsParam<?>> setters;

		public JaxrsBeanParam(T p, ClassModel clazz, Map<FieldModel, JaxrsParam<?>> fields,
				Map<MethodModel, JaxrsParam<?>> setters) {
			super(p, "o", null);
			this.clazz = clazz;
			this.fields = fields;
			this.setters = setters;
		}

		@Override
		public void collect(Consumer<JaxrsParam<?>> c) {
			for (JaxrsParam<?> p : fields.values())
				p.collect(c);
			for (JaxrsParam<?> p : setters.values())
				p.collect(c);
			c.accept(this);
		}
	}

	public static class JaxrsPathParam<T extends WithName & WithAnnotation & WithType> extends JaxrsParam<T> {

		public JaxrsPathParam(T p, String part) {
			super(p, "p", part);
		}
	}

	public static class JaxrsQueryParam<T extends WithName & WithAnnotation & WithType> extends JaxrsParam<T> {

		public JaxrsQueryParam(T p, String query) {
			super(p, "q", query);
		}

	}

	public static class JaxrsFormParam<T extends WithName & WithAnnotation & WithType> extends JaxrsParam<T> {

		public JaxrsFormParam(T p, String param) {
			super(p, "f", param);
		}
	}

	public static class JaxrsHeaderParam<T extends WithName & WithAnnotation & WithType> extends JaxrsParam<T> {

		public JaxrsHeaderParam(T p, String header) {
			super(p, "h", header);
		}
	}

	public static class JaxrsCookieParam<T extends WithName & WithAnnotation & WithType> extends JaxrsParam<T> {

		public JaxrsCookieParam(T p, String cookie) {
			super(p, "c", cookie);
		}
	}

	public static class JaxrsMatrixParam<T extends WithName & WithAnnotation & WithType> extends JaxrsParam<T> {

		public JaxrsMatrixParam(T p, String param) {
			super(p, "m", param);
		}
	}

	public static class JaxrsBodyParam<T extends WithName & WithAnnotation & WithType> extends JaxrsParam<T> {

		public JaxrsBodyParam(T p) {
			super(p, "b", null);
		}
	}
}
