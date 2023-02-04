/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import unknow.server.maven.jaxws.binding.XmlObject.Factory;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.TypeModel;

/**
 * load XmlType
 * 
 * @author unknow
 */
public class XmlTypeLoader {
	private static final Map<String, XmlType<?>> DEFAULT = new HashMap<>();
	static {
		DEFAULT.put("java.lang.String", XmlDefaultType.XmlString);
		DEFAULT.put("boolean", XmlTypePrimitive.XmlBoolean);
		DEFAULT.put("byte", XmlTypePrimitive.XmlByte);
		DEFAULT.put("char", XmlTypePrimitive.XmlChar);
		DEFAULT.put("short", XmlTypePrimitive.XmlShort);
		DEFAULT.put("int", XmlTypePrimitive.XmlInt);
		DEFAULT.put("long", XmlTypePrimitive.XmlLong);
		DEFAULT.put("float", XmlTypePrimitive.XmlFloat);
		DEFAULT.put("double", XmlTypePrimitive.XmlDouble);
		DEFAULT.put("java.lang.Boolean", XmlDefaultType.XmlBoolean);
		DEFAULT.put("java.lang.Byte", XmlDefaultType.XmlByte);
		DEFAULT.put("java.lang.Character", XmlDefaultType.XmlChar);
		DEFAULT.put("java.lang.Short", XmlDefaultType.XmlShort);
		DEFAULT.put("java.lang.Integer", XmlDefaultType.XmlInt);
		DEFAULT.put("java.lang.Long", XmlDefaultType.XmlLong);
		DEFAULT.put("java.lang.Float", XmlDefaultType.XmlFloat);
		DEFAULT.put("java.lang.Double", XmlDefaultType.XmlDouble);
		DEFAULT.put("java.math.BigInteger", XmlDefaultType.XmlBigInteger);
		DEFAULT.put("java.math.BigDecimal", XmlDefaultType.XmlBigDecimal);
		DEFAULT.put("java.time.LocalDate", XmlTypeDate.LOCAL_DATE);
		DEFAULT.put("java.time.LocalTime", XmlTypeDate.LOCAL_TIME);
		DEFAULT.put("java.time.LocalDateTime", XmlTypeDate.LOCAL_DATETIME);
		DEFAULT.put("java.time.OffsetTime", XmlTypeDate.OFFSET_TIME);
		DEFAULT.put("java.time.OffsetDateTime", XmlTypeDate.OFFSET_DATETIME);
		DEFAULT.put("java.time.ZonedDateTime", XmlTypeDate.ZONED_DATETIME);
	}

	private final Map<String, XmlType<?>> loaded;

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
	public XmlType<?> get(TypeModel type) {
		String name = type.name();
		XmlType<?> xmlType = loaded.get(name);
		if (xmlType == null)
			loaded.put(name, xmlType = create(type));
		return xmlType;
	}

	private XmlType<?> create(TypeModel type) {
//		if (type.isArray())
//			return new XmlArray(get(type.asArray().type()));
		if (type.isEnum())
			return new XmlEnum(type.asEnum(), getNs(type), getName(type), "enum" + loaded.size() + "$");
		if (type.isClass()) {
			// TODO Collection
			// TODO Map ?

			return createObject(type.asClass());
		}
		throw new RuntimeException("invalid type declaration " + type.name());
	}

	private XmlObject createObject(ClassModel c) {
		// TODO get namespace data ?
		String namespace = "";
		String name = c.simpleName();

		Factory factory = null;
		Optional<AnnotationModel> o = c.annotation(jakarta.xml.bind.annotation.XmlType.class);
		String cl = o.flatMap(a -> a.value("factoryClass")).orElse(null);
		String method = o.flatMap(a -> a.value("factoryMethod")).orElse("");

		if (!method.isEmpty()) {
			if (jakarta.xml.bind.annotation.XmlType.DEFAULT.class.getCanonicalName().equals(cl))
				factory = new Factory(c.name(), method);
			else
				factory = new Factory(cl, method);
		}
		name = o.flatMap(a -> a.value("name")).map(v -> "##default".equals(v) ? null : v).orElse(name);
		namespace = o.flatMap(a -> a.value("namespace")).map(v -> "##default".equals(v) ? null : v).orElse(namespace);

		return new XmlObject(c, namespace, name, factory, this);
	}

	private static String getNs(TypeModel c) {
		// TODO get namespace data from package annotation
		String ns = "";
		return c.annotation(jakarta.xml.bind.annotation.XmlType.class).flatMap(a -> a.value("namespace")).map(v -> "##default".equals(v) ? null : v).orElse(ns);
	}

	private static String getName(TypeModel c) {
		String name = c.simpleName();
		return c.annotation(jakarta.xml.bind.annotation.XmlType.class).flatMap(a -> a.value("name")).map(v -> "##default".equals(v) ? null : v).orElse(name);
	}
}
