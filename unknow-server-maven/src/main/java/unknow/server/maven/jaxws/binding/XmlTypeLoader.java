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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import unknow.server.maven.jaxws.binding.XmlEnum.XmlEnumEntry;
import unknow.server.maven.jaxws.binding.XmlObject.Factory;
import unknow.server.maven.jaxws.binding.XmlObject.XmlElem;
import unknow.server.maven.jaxws.binding.XmlObject.XmlField;
import unknow.server.maven.jaxws.binding.XmlType.XmlList;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.EnumModel;
import unknow.server.maven.model.EnumModel.EnumConstant;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.TypeModel;

/**
 * load XmlType
 * 
 * @author unknow
 */
public class XmlTypeLoader {
	private static final Map<String, XmlType> DEFAULT = new HashMap<>();
	static {
		DEFAULT.put("java.lang.String", XmlType.XmlString);
		DEFAULT.put("boolean", XmlType.XmlBoolean);
		DEFAULT.put("byte", XmlType.XmlByte);
		DEFAULT.put("char", XmlType.XmlChar);
		DEFAULT.put("short", XmlType.XmlShort);
		DEFAULT.put("int", XmlType.XmlInt);
		DEFAULT.put("long", XmlType.XmlLong);
		DEFAULT.put("float", XmlType.XmlFloat);
		DEFAULT.put("double", XmlType.XmlDouble);
		DEFAULT.put("java.lang.Boolean", XmlType.XmlBoolean);
		DEFAULT.put("java.lang.Byte", XmlType.XmlByte);
		DEFAULT.put("java.lang.Character", XmlType.XmlChar);
		DEFAULT.put("java.lang.Short", XmlType.XmlShort);
		DEFAULT.put("java.lang.Integer", XmlType.XmlInt);
		DEFAULT.put("java.lang.Long", XmlType.XmlLong);
		DEFAULT.put("java.lang.Float", XmlType.XmlFloat);
		DEFAULT.put("java.lang.Double", XmlType.XmlDouble);
		DEFAULT.put("java.math.BigInteger", XmlType.XmlBigInteger);
		DEFAULT.put("java.math.BigDecimal", XmlType.XmlBigDecimal);
		DEFAULT.put("java.time.LocalDate", XmlTypeDate.LOCAL_DATE);
		DEFAULT.put("java.time.LocalTime", XmlTypeDate.LOCAL_TIME);
		DEFAULT.put("java.time.LocalDateTime", XmlTypeDate.LOCAL_DATETIME);
		DEFAULT.put("java.time.OffsetTime", XmlTypeDate.OFFSET_TIME);
		DEFAULT.put("java.time.OffsetDateTime", XmlTypeDate.OFFSET_DATETIME);
		DEFAULT.put("java.time.ZonedDateTime", XmlTypeDate.ZONED_DATETIME);
	}

	private final Map<String, XmlType> loaded;

	/**
	 * create new XmlTypeLoader
	 * 
	 */
	public XmlTypeLoader() {
		this.loaded = new HashMap<>(DEFAULT);
	}

	/**
	 * get an XmlType
	 * 
	 * @param type type to convert
	 * @return the XmlType
	 */
	public XmlType get(TypeModel type) {
		String name = type.name();
		XmlType xmlType = loaded.get(name);
		if (xmlType == null) {
			XmlTypeWrapper w = new XmlTypeWrapper();
			loaded.put(name, w);
			xmlType = create(type);
			w.setDelegate(xmlType);
			loaded.put(name, xmlType);
		}
		return xmlType;
	}

	private XmlType create(TypeModel type) {
		if (type.isArray())
			return new XmlList(get(type.asArray().type()));
		if (type.isEnum())
			return createEnum(type.asEnum());
		if (type.isClass()) {
			// TODO Collection
			// TODO Map ?

			return createObject(type.asClass());
		}
		throw new RuntimeException("invalid type declaration " + type.name());
	}

	/**
	 * @param enumDeclaration
	 * @return
	 */
	private XmlType createEnum(EnumModel type) {
		List<XmlEnumEntry> entries = new ArrayList<>();
		for (EnumConstant e : type.entries())
			entries.add(new XmlEnumEntry(e.name(), e.annotation(XmlEnumValue.class).value("value").orElse(e.name())));
		return new XmlEnum(type.name(), entries, getSchema(type), "enum" + loaded.size() + "$");
	}

	private XmlObject createObject(ClassModel c) {
		// TODO get namespace data ?
		String namespace = "";
		String name = c.simpleName();

		AnnotationModel a = c.annotation(XmlAccessorType.class);
		XmlAccessType type = XmlAccessType.PUBLIC_MEMBER;
		if (a != null)
			type = a.value("value").map(XmlAccessType::valueOf).orElse(XmlAccessType.PUBLIC_MEMBER);

		Factory factory = null;
		a = c.annotation(javax.xml.bind.annotation.XmlType.class);
		List<String> propOrder = null;
		if (a != null) {
			String cl = a.value("factoryClass").orElse(null);
			String method = a.value("factoryMethod").orElse("");

			if (!method.isEmpty()) {
				if (javax.xml.bind.annotation.XmlType.DEFAULT.class.getCanonicalName().equals(cl))
					factory = new Factory(c.name(), method);
				else
					factory = new Factory(cl, method);
			}
			name = a.value("name").map(v -> "##default".equals(v) ? null : v).orElse(name);
			namespace = a.value("namespace").map(v -> "##default".equals(v) ? null : v).orElse(namespace);
			propOrder = a.values("propOrder").map(Arrays::asList).orElse(null);
		}

		Map<String, FieldModel> fields = new HashMap<>();

		for (FieldModel f : c.fields()) {
			if (f.isTransient() || f.annotation(XmlTransient.class) != null)
				continue;

			a = f.annotation(XmlElement.class);
			if (a == null)
				a = f.annotation(XmlAttribute.class);
			if (a == null)
				a = f.annotation(XmlValue.class);

			if (a != null || type == XmlAccessType.FIELD || type == XmlAccessType.PUBLIC_MEMBER && f.isPublic()) {
				String setter = "set" + Character.toUpperCase(f.name().charAt(0)) + f.name().substring(1);
				String getter = "get" + Character.toUpperCase(f.name().charAt(0)) + f.name().substring(1);
				// TODO check also is* for boolean

				Optional<MethodModel> s = c.methods().stream().filter(m -> m.name().equals(setter))
						.filter(m -> m.parameters().size() == 1 && m.parameters().get(0).type().name().equals(f.type().name()))
						.findFirst();
				Optional<MethodModel> g = c.methods().stream().filter(m -> m.name().equals(getter)).filter(m -> m.parameters().size() == 0)
						.findFirst();
				if (!s.isPresent())
					throw new RuntimeException("missing setter for '" + f.name() + "' field in '" + c.name() + "' class");
				if (!g.isPresent()) // TODO check is* if boolean
					throw new RuntimeException("missing setter for '" + f.name() + "' field in '" + c.name() + "' class");

				fields.put(f.name(), f);
			}
		}

		List<XmlField> attrs = new ArrayList<>();
		List<XmlField> elems = new ArrayList<>();
		XmlElem value = null;
		for (MethodModel m : c.methods()) {
			// skip non getter
			if (m.parameters().size() > 0 || !m.name().startsWith("get"))
				continue;
			String n = Character.toLowerCase(m.name().charAt(3)) + m.name().substring(4);

			AnnotationModel elem = m.annotation(XmlElement.class);
			AnnotationModel attr = m.annotation(XmlAttribute.class);
			AnnotationModel v = m.annotation(XmlValue.class);

			FieldModel f = fields.get(n);
			if (f != null && elem == null && attr == null && v == null) {
				elem = f.annotation(XmlElement.class);
				attr = f.annotation(XmlAttribute.class);
				v = f.annotation(XmlValue.class);
			}

			if (elem != null || attr != null || v != null || type == XmlAccessType.FIELD && f != null || type == XmlAccessType.PUBLIC_MEMBER && m.isPublic()) {
				String setter = "set" + m.name().substring(3);

				Optional<MethodModel> s = c.methods().stream().filter(e -> e.name().equals(setter))
						.filter(e -> e.parameters().size() == 1 && e.parameters().get(0).type().name().equals(m.type().name())).findFirst();
				if (!s.isPresent())
					throw new RuntimeException("missing setter for '" + n + "' field in '" + c.name() + "' class");
				XmlType t = get(m.type());
				if (v != null) {
					if (value != null)
						throw new RuntimeException("multiple value for '" + c.name() + "'");
					value = new XmlElem(t, m.name(), setter);
				} else if (attr != null) {
					if (!t.isSimple())
						throw new RuntimeException("only simple type allowed in attribute in '" + c.name() + "'");
					n = attr.value("name").map(i -> "##default".equals(i) ? null : i).orElse(n);
					String ns = attr.value("namespace").map(i -> "##default".equals(i) ? null : i).orElse("");
					attrs.add(new XmlField(t, m.name(), setter, n, ns));
				} else {
					String ns = namespace;
					if (elem != null) {
						n = elem.value("name").map(i -> "##default".equals(i) ? null : i).orElse(n);
						ns = elem.value("namespace").map(i -> "##default".equals(i) ? null : i).orElse(namespace);
					}
					elems.add(new XmlField(t, m.name(), setter, n, ns));
				}
			}
		}
		if (propOrder != null) {
			List<String> order = propOrder;
			Collections.sort(elems, (o, b) -> {
				String an = Character.toLowerCase(o.getter.charAt(3)) + o.getter.substring(4);
				String bn = Character.toLowerCase(b.getter.charAt(3)) + b.getter.substring(4);
				int ai = order.indexOf(an);
				int bi = order.indexOf(bn);
				if (bi >= 0 && ai >= 0)
					return ai - bi;
				if (ai < 0 && bi < 0)
					return 0; // TODO default ordering @XmlAccessorOrder
				if (bi < 0)
					return -1;
				return 1;
			});
		}
		return new XmlObject(c.name(), factory, attrs, elems, value, getSchema(c));
	}

	/**
	 * @param c
	 * @return
	 */
	private static SchemaData getSchema(TypeModel c) {
		// TODO get namespace data from package annotation
		String ns = "";
		String name = c.simpleName();

		String rootElem = null;
		String rootNs = null;
		AnnotationModel a = c.annotation(javax.xml.bind.annotation.XmlRootElement.class);
		if (a != null) {
			rootElem = a.value("name").map(v -> "##default".equals(v) ? null : v).orElse(name);
			rootNs = a.value("namespace").map(v -> "##default".equals(v) ? null : v).orElse(ns);
		}

		a = c.annotation(javax.xml.bind.annotation.XmlType.class);
		if (a != null) {
			name = a.value("name").map(v -> "##default".equals(v) ? null : v).orElse(name);
			ns = a.value("namespace").map(v -> "##default".equals(v) ? null : v).orElse(ns);
		}

		return new SchemaData(name, ns, rootElem, rootNs);
	}
}
