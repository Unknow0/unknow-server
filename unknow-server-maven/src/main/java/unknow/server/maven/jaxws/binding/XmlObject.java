/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;

import jakarta.xml.bind.annotation.XmlAccessOrder;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorOrder;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlValue;
import unknow.server.maven.TypeCache;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.PrimitiveModel;

/**
 * @author unknow
 */
public class XmlObject extends XmlType<ClassModel> {
	private final XmlTypeLoader loader;
	private final Factory factory;
	protected List<XmlField<?>> elems;
	protected List<XmlField<?>> attrs;
	protected XmlField<?> value;

	public XmlObject(ClassModel clazz, String ns, String name, Factory factory, XmlTypeLoader loader) {
		super(clazz, ns, name);
		this.factory = factory;
		this.loader = loader;
	}

	public String factoryClazz() {
		return factory == null ? null : factory.clazz;
	}

	public String factoryMethod() {
		return factory == null ? null : factory.method;
	}

	@Override
	public Expression fromString(TypeCache types, Expression v) {
		return new CastExpr(types.get(javaType().name()), v);
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	public List<XmlField<?>> elems() {
		if (elems == null)
			init();
		return elems;
	}

	public List<XmlField<?>> attrs() {
		if (elems == null)
			init();
		return attrs;
	}

	public XmlField<?> value() {
		if (elems == null)
			init();
		return value;
	}

	private void init() {
		ClassModel c = javaType();

		XmlAccessType type = c.annotation(XmlAccessorType.class).flatMap(a -> a.value()).map(XmlAccessType::valueOf).orElse(XmlAccessType.PUBLIC_MEMBER);

		List<String> propOrder = c.annotation(jakarta.xml.bind.annotation.XmlType.class).flatMap(a -> a.values("propOrder")).map(Arrays::asList).orElse(Collections.emptyList());

		Map<String, FieldModel> fields = new HashMap<>();

		for (FieldModel f : c.fields()) {
			if (f.isStatic() || f.isTransient() || f.annotation(XmlTransient.class) != null)
				continue;

			boolean annoted = f.annotation(XmlElement.class).or(() -> f.annotation(XmlAttribute.class)).or(() -> f.annotation(XmlValue.class)).isPresent();

			if (annoted && type != XmlAccessType.FIELD && (type != XmlAccessType.PUBLIC_MEMBER || !f.isPublic()))
				continue;
			String setter = "set" + Character.toUpperCase(f.name().charAt(0)) + f.name().substring(1);
			String getter = "get" + Character.toUpperCase(f.name().charAt(0)) + f.name().substring(1);

			Optional<MethodModel> s = c.methods().stream().filter(m -> m.name().equals(setter))
					.filter(m -> m.parameters().size() == 1 && m.parameters().get(0).type().name().equals(f.type().name())).findFirst();
			Optional<MethodModel> g = c.methods().stream().filter(m -> m.name().equals(getter)).filter(m -> m.parameters().size() == 0).findFirst();
			if (!s.isPresent())
				throw new RuntimeException("missing setter for '" + f.name() + "' field in '" + c.name() + "' class");
			if (!g.isPresent() && (f.type() == PrimitiveModel.BOOLEAN || f.type().name().equals("java.lang.Boolean"))) {
				String is = "is" + Character.toUpperCase(f.name().charAt(0)) + f.name().substring(1);
				g = c.methods().stream().filter(m -> m.name().equals(is)).filter(m -> m.parameters().size() == 0).findFirst();
			}
			if (!g.isPresent())
				throw new RuntimeException("missing setter for '" + f.name() + "' field in '" + c.name() + "' class");

			fields.put(f.name(), f);
		}

		attrs = new ArrayList<>();
		elems = new ArrayList<>();
		for (MethodModel m : c.methods()) {
			// skip non getter
			if (m.parameters().size() > 0 || !m.name().startsWith("get"))
				continue;
			String n = Character.toLowerCase(m.name().charAt(3)) + m.name().substring(4);

			Optional<AnnotationModel> elem = m.annotation(XmlElement.class);
			Optional<AnnotationModel> attr = m.annotation(XmlAttribute.class);
			Optional<AnnotationModel> v = m.annotation(XmlValue.class);

			FieldModel f = fields.get(n);
			if (f != null && elem.isEmpty() && attr.isEmpty() && v.isEmpty()) {
				elem = f.annotation(XmlElement.class);
				attr = f.annotation(XmlAttribute.class);
				v = f.annotation(XmlValue.class);
			}

			if (elem.isPresent() || attr.isPresent() || v.isPresent() || type == XmlAccessType.FIELD && f != null || type == XmlAccessType.PUBLIC_MEMBER && m.isPublic()) {
				String setter = "set" + m.name().substring(3);

				Optional<MethodModel> s = c.methods().stream().filter(e -> e.name().equals(setter))
						.filter(e -> e.parameters().size() == 1 && e.parameters().get(0).type().name().equals(m.type().name())).findFirst();
				if (!s.isPresent())
					throw new RuntimeException("missing setter for '" + n + "' field in '" + c.name() + "' class");
				XmlType<?> t = loader.get(m.type());
				if (v.isPresent()) {
					if (value != null)
						throw new RuntimeException("multiple value for '" + c.name() + "'");
					value = new XmlField<>(t, "", "", m.name(), setter);
				} else if (attr.isPresent()) {
					if (!t.isSimple())
						throw new RuntimeException("only simple type allowed in attribute in '" + c.name() + "'");
					n = attr.flatMap(a -> a.value("name")).map(i -> "##default".equals(i) ? null : i).orElse(n);
					String ns = attr.flatMap(a -> a.value("namespace")).map(i -> "##default".equals(i) ? null : i).orElse("");
					attrs.add(new XmlField<>(t, ns, n, m.name(), setter));
				} else {
					n = elem.flatMap(a -> a.value("name")).map(i -> "##default".equals(i) ? null : i).orElse(n);
					String ns = elem.flatMap(a -> a.value("namespace")).map(i -> "##default".equals(i) ? null : i).orElse("");
					elems.add(new XmlField<>(t, ns, n, m.name(), setter));
				}
			}
		}

		XmlAccessOrder defaultOrder = c.annotation(XmlAccessorOrder.class).flatMap(a -> a.value()).map(a -> XmlAccessOrder.valueOf(a)).orElse(XmlAccessOrder.UNDEFINED);

		if (!propOrder.isEmpty() || defaultOrder != XmlAccessOrder.UNDEFINED) {
			Collections.sort(elems, (o, b) -> {
				String an = Character.toLowerCase(o.getter.charAt(3)) + o.getter.substring(4);
				String bn = Character.toLowerCase(b.getter.charAt(3)) + b.getter.substring(4);
				int ai = propOrder.indexOf(an);
				int bi = propOrder.indexOf(bn);
				if (bi >= 0 && ai >= 0)
					return ai - bi;
				if (ai < 0 && bi < 0)
					return defaultOrder == XmlAccessOrder.ALPHABETICAL ? an.compareTo(bn) : 0;
				if (bi < 0)
					return -1;
				return 1;
			});
		}
	}

	public static class Factory {
		public final String clazz;
		public final String method;

		/**
		 * create new Factory
		 * 
		 * @param clazz
		 * @param method
		 */
		public Factory(String clazz, String method) {
			this.clazz = clazz;
			this.method = method;
		}
	}

	public static class XmlField<T extends XmlType<?>> extends XmlElem<T> {
		public final String getter;
		public final String setter;

		public XmlField(T type, String ns, String name, String getter, String setter) {
			super(type, ns, name);
			this.getter = getter;
			this.setter = setter;
		}
	}
}
