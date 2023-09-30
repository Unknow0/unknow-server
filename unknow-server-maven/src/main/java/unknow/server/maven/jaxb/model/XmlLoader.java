/**
 * 
 */
package unknow.server.maven.jaxb.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.annotation.XmlAccessOrder;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorOrder;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlValue;
import unknow.server.maven.jaxb.model.XmlElements.XmlGroup;
import unknow.server.maven.jaxb.model.XmlTypeComplex.Factory;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.FieldModel;
import unknow.server.maven.model.MethodModel;
import unknow.server.maven.model.PrimitiveModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.jvm.JvmModelLoader;

/**
 * @author unknow
 */
public class XmlLoader {
	private static final Logger logger = LoggerFactory.getLogger(XmlLoader.class);

	public static final XmlTypeSimple BOOLEAN = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "boolean"), PrimitiveModel.BOOLEAN);
	public static final XmlTypeSimple BYTE = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "byte"), PrimitiveModel.BYTE);
	public static final XmlTypeSimple SHORT = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "short"), PrimitiveModel.SHORT);
	public static final XmlTypeSimple INT = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "int"), PrimitiveModel.INT);
	public static final XmlTypeSimple LONG = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "long"), PrimitiveModel.LONG);
	public static final XmlTypeSimple FLOAT = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "float"), PrimitiveModel.FLOAT);
	public static final XmlTypeSimple DOUBLE = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "double"), PrimitiveModel.DOUBLE);
	public static final XmlTypeSimple CHAR = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "string"), PrimitiveModel.CHAR);
	public static final XmlTypeSimple STRING = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "string"), JvmModelLoader.GLOBAL.get(String.class.getName()));
	public static final XmlTypeSimple BIGINT = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "integer"), JvmModelLoader.GLOBAL.get(BigInteger.class.getName()));
	public static final XmlTypeSimple BIGDEC = new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "decimal"), JvmModelLoader.GLOBAL.get(BigDecimal.class.getName()));

	private final Map<String, XmlType> types = new HashMap<>();

	public XmlLoader() {
		types.put(PrimitiveModel.BOOLEAN.name(), BOOLEAN);
		types.put(PrimitiveModel.BOOLEAN.boxed().name(), BOOLEAN);
		types.put(PrimitiveModel.BYTE.name(), BYTE);
		types.put(PrimitiveModel.BYTE.boxed().name(), BYTE);
		types.put(PrimitiveModel.SHORT.name(), SHORT);
		types.put(PrimitiveModel.SHORT.boxed().name(), SHORT);
		types.put(PrimitiveModel.INT.name(), INT);
		types.put(PrimitiveModel.INT.boxed().name(), INT);
		types.put(PrimitiveModel.LONG.name(), LONG);
		types.put(PrimitiveModel.LONG.boxed().name(), LONG);
		types.put(PrimitiveModel.FLOAT.name(), FLOAT);
		types.put(PrimitiveModel.FLOAT.boxed().name(), FLOAT);
		types.put(PrimitiveModel.DOUBLE.name(), DOUBLE);
		types.put(PrimitiveModel.DOUBLE.boxed().name(), DOUBLE);
		types.put(PrimitiveModel.CHAR.name(), CHAR);
		types.put(PrimitiveModel.CHAR.boxed().name(), CHAR);
		types.put(String.class.getName(), STRING);
		types.put(BigInteger.class.getName(), BIGINT);
		types.put(BigDecimal.class.getName(), BIGDEC);
	}

	public Set<Entry<String, XmlType>> entries() {
		return types.entrySet();
	}

	public Collection<XmlType> types() {
		return new HashSet<>(types.values());
	}

	public XmlType add(TypeModel type) {
		XmlType xmlType = types.get(type.name());
		if (xmlType == null) {
			logger.info("adding bind for {}", type);
			types.put(type.name(), xmlType = create(type));
		}
		return xmlType;
	}

	private XmlType create(TypeModel type) {
		if (type.isPrimitive()) // should not happen
			throw new RuntimeException("Unsupported primitive " + type);
		if (type.isEnum()) {
			// TODO get type
//			type.annotation(jakarta.xml.bind.annotation.XmlEnum.class);
			return new XmlEnum(qname(type), type.asEnum(), XmlLoader.STRING);
		}
		if (type.isArray())
			return new XmlCollection(type, add(type.asArray().type()));

		ClassModel cl = type.asClass();
		if (cl.isBoxedPrimitive()) // should not happen
			throw new RuntimeException("Unsupported boxed primitive " + type);
		if (cl.isAssignableTo(String.class))
			return STRING;

		if (cl.isAssignableTo(Duration.class.getName()))
			return new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "duration"), cl);
		if (cl.isAssignableTo(LocalDate.class))
			return new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "date"), cl);
		if (cl.isAssignableTo(LocalTime.class) || cl.isAssignableTo(OffsetTime.class))
			return new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "time"), cl);
		if (cl.isAssignableTo(LocalDateTime.class) || cl.isAssignableTo(OffsetDateTime.class) || cl.isAssignableTo(ZonedDateTime.class))
			return new XmlTypeSimple(new QName("http://www.w3.org/2001/XMLSchema", "dateTime"), cl);

		ClassModel col = cl.ancestor(Collection.class.getName());
		if (col != null)
			return new XmlCollection(type, add(col.parameter(0).type()));

		return createObject(cl);
	}

	private XmlType createObject(ClassModel c) {
		XmlAccessType type = c.annotation(XmlAccessorType.class).flatMap(a -> a.value()).map(a -> XmlAccessType.valueOf(a.asLiteral())).orElse(XmlAccessType.PUBLIC_MEMBER);

		Map<String, FieldModel> fields = new HashMap<>();

		for (FieldModel f : c.fields()) {
			if (f.isStatic() || f.isTransient() || f.annotation(XmlTransient.class).isPresent())
				continue;

			boolean annoted = f.annotation(jakarta.xml.bind.annotation.XmlElement.class).or(() -> f.annotation(jakarta.xml.bind.annotation.XmlAttribute.class)).or(() -> f.annotation(XmlValue.class)).isPresent();

			if (annoted && type != XmlAccessType.FIELD && (type != XmlAccessType.PUBLIC_MEMBER || !f.isPublic()))
				continue;
			String setter = "set" + Character.toUpperCase(f.name().charAt(0)) + f.name().substring(1);
			String getter = "get" + Character.toUpperCase(f.name().charAt(0)) + f.name().substring(1);

			Optional<MethodModel> s = c.method(setter, f.type());
			Optional<MethodModel> g = c.method(getter);
			if (!s.isPresent())
				throw new RuntimeException("missing setter for '" + f.name() + "' field in '" + c.name() + "' class");
			if (!g.isPresent() && (f.type() == PrimitiveModel.BOOLEAN || f.type().name().equals("java.lang.Boolean"))) {
				String is = "is" + Character.toUpperCase(f.name().charAt(0)) + f.name().substring(1);
				g = c.method(is);
			}
			if (!g.isPresent())
				throw new RuntimeException("missing setter for '" + f.name() + "' field in '" + c.name() + "' class");

			fields.put(f.name(), f);
		}

		List<XmlElement> attrs = new ArrayList<>();
		List<XmlElement> elems = new ArrayList<>();
		XmlElement value = null;
		for (MethodModel m : c.methods()) {
			// skip non getter
			if (m.parameters().size() > 0 || !m.name().startsWith("get"))
				continue;
			String n = Character.toLowerCase(m.name().charAt(3)) + m.name().substring(4);

			Optional<AnnotationModel> elem = m.annotation(jakarta.xml.bind.annotation.XmlElement.class);
			Optional<AnnotationModel> attr = m.annotation(jakarta.xml.bind.annotation.XmlAttribute.class);
			Optional<AnnotationModel> v = m.annotation(XmlValue.class);

			FieldModel f = fields.get(n);
			if (f != null && elem.isEmpty() && attr.isEmpty() && v.isEmpty()) {
				elem = f.annotation(jakarta.xml.bind.annotation.XmlElement.class);
				attr = f.annotation(jakarta.xml.bind.annotation.XmlAttribute.class);
				v = f.annotation(XmlValue.class);
			}

			if (elem.isPresent() || attr.isPresent() || v.isPresent() || type == XmlAccessType.FIELD && f != null || type == XmlAccessType.PUBLIC_MEMBER && m.isPublic()) {
				String setter = "set" + m.name().substring(3);

				TypeModel t = m.type();
				Optional<MethodModel> s = c.method(setter, t);
				if (!s.isPresent())
					throw new RuntimeException("missing setter for '" + n + "' field in '" + c.name() + "' class");
				if (v.isPresent()) {
					if (value != null)
						throw new RuntimeException("multiple value for '" + c.name() + "'");
					if (!(add(t) instanceof XmlTypeSimple))
						throw new RuntimeException("only simple type allowed in value '" + c.name() + "'");
					value = new XmlElement(this, null, t, m.name(), setter);
				} else if (attr.isPresent()) {
					if (!(add(t) instanceof XmlTypeSimple))
						throw new RuntimeException("only simple type allowed in attribute in '" + c.name() + "'");
					n = attr.flatMap(a -> a.member("name")).map(a -> a.asLiteral()).map(i -> "##default".equals(i) ? null : i).orElse(n);
					String ns = attr.flatMap(a -> a.member("namespace")).map(a -> a.asLiteral()).map(i -> "##default".equals(i) ? null : i).orElse("");
					attrs.add(new XmlElement(this, new QName(ns, n), t, m.name(), setter));
				} else {
					n = elem.flatMap(a -> a.member("name")).map(a -> a.asLiteral()).map(i -> "##default".equals(i) ? null : i).orElse(n);
					String ns = elem.flatMap(a -> a.member("namespace")).map(a -> a.asLiteral()).map(i -> "##default".equals(i) ? null : i).orElse("");
					elems.add(new XmlElement(this, new QName(ns, n), t, m.name(), setter));
				}
			}
		}

		if (value != null && !elems.isEmpty())
			throw new RuntimeException("Mixed content not supported in " + c);

		XmlElements elements;
		XmlAccessOrder defaultOrder = c.annotation(XmlAccessorOrder.class).flatMap(a -> a.value()).map(a -> a.asLiteral()).map(a -> XmlAccessOrder.valueOf(a)).orElse(XmlAccessOrder.UNDEFINED);
		List<String> propOrder = c.annotation(jakarta.xml.bind.annotation.XmlType.class).flatMap(a -> a.member("propOrder")).map(a -> a.asArrayLiteral()).map(Arrays::asList).orElse(Collections.emptyList());
		if (propOrder.isEmpty()) {
			if (defaultOrder == XmlAccessOrder.ALPHABETICAL)
				Collections.sort(elems, (a, b) -> a.getter().compareTo(b.getter()));
			elements = new XmlElements(XmlGroup.all, elems);
		} else {
			Collections.sort(elems, (o, b) -> {
				String an = Character.toLowerCase(o.getter().charAt(3)) + o.getter().substring(4);
				String bn = Character.toLowerCase(b.getter().charAt(3)) + b.getter().substring(4);
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
			elements = new XmlElements(XmlGroup.sequence, elems);
		}

		Optional<AnnotationModel> a = c.annotation(jakarta.xml.bind.annotation.XmlType.class);
		Factory f = new Factory(
				a.flatMap(v -> v.member("factoryClass")).map(v -> v.asClass().asClass()).filter(v -> !v.isAssignableTo(jakarta.xml.bind.annotation.XmlType.DEFAULT.class.getName())).orElse(c),
				a.flatMap(v -> v.member("factoryMethod")).map(v -> v.asLiteral()).orElse(""));
		return new XmlTypeComplex(qname(c), c, f, attrs, elements, value);
	}

	private static QName qname(TypeModel type) {
		Optional<AnnotationModel> a = type.annotation(jakarta.xml.bind.annotation.XmlType.class);

		String name = type.simpleName();
		String ns = ""; // TODO get from package annotation
		if (a.isPresent()) {
			name = a.flatMap(v -> v.member("name")).map(v -> v.asLiteral()).filter(v -> !v.equals("##default")).orElse(name);
			ns = a.flatMap(v -> v.member("namespace")).map(v -> v.asLiteral()).filter(v -> !v.equals("##default")).orElse(ns);
		}
		return new QName(name, ns);
	}
}