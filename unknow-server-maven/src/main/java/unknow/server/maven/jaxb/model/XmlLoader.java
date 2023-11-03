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
import java.time.Period;
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
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlValue;
import unknow.server.maven.jaxb.model.XmlElements.XmlGroup;
import unknow.server.maven.jaxb.model.XmlElements.XmlGroupElements;
import unknow.server.maven.jaxb.model.XmlElements.XmlSimpleElements;
import unknow.server.maven.jaxb.model.XmlTypeComplex.Factory;
import unknow.server.maven.model.AnnotationModel;
import unknow.server.maven.model.BeanProperty;
import unknow.server.maven.model.ClassModel;
import unknow.server.maven.model.PrimitiveModel;
import unknow.server.maven.model.TypeModel;
import unknow.server.maven.model.jvm.JvmModelLoader;

/**
 * @author unknow
 */
public class XmlLoader {
	private static final Logger logger = LoggerFactory.getLogger(XmlLoader.class);

	private static final String NAMESPACE = "namespace";
	private static final String DEFAULT = "##default";

	public static final String XS = "http://www.w3.org/2001/XMLSchema";

	public static final XmlType ANY = new XmlTypeComplex(new QName(XS, "any"), JvmModelLoader.GLOBAL.get("java.lang.Object").asClass(),
			new Factory(JvmModelLoader.GLOBAL.get("java.lang.Object").asClass(), ""), Collections.emptyList(), null, null);

	public static final XmlTypeSimple BOOLEAN = new XmlTypeSimple(new QName(XS, "boolean"), PrimitiveModel.BOOLEAN);
	public static final XmlTypeSimple BYTE = new XmlTypeSimple(new QName(XS, "byte"), PrimitiveModel.BYTE);
	public static final XmlTypeSimple SHORT = new XmlTypeSimple(new QName(XS, "short"), PrimitiveModel.SHORT);
	public static final XmlTypeSimple INT = new XmlTypeSimple(new QName(XS, "int"), PrimitiveModel.INT);
	public static final XmlTypeSimple LONG = new XmlTypeSimple(new QName(XS, "long"), PrimitiveModel.LONG);
	public static final XmlTypeSimple FLOAT = new XmlTypeSimple(new QName(XS, "float"), PrimitiveModel.FLOAT);
	public static final XmlTypeSimple DOUBLE = new XmlTypeSimple(new QName(XS, "double"), PrimitiveModel.DOUBLE);
	public static final XmlTypeSimple CHAR = new XmlTypeSimple(new QName(XS, "string"), PrimitiveModel.CHAR);
	public static final XmlTypeSimple STRING = new XmlTypeSimple(new QName(XS, "string"), JvmModelLoader.GLOBAL.get(String.class.getName()));
	public static final XmlTypeSimple BIGINT = new XmlTypeSimple(new QName(XS, "integer"), JvmModelLoader.GLOBAL.get(BigInteger.class.getName()));
	public static final XmlTypeSimple BIGDEC = new XmlTypeSimple(new QName(XS, "decimal"), JvmModelLoader.GLOBAL.get(BigDecimal.class.getName()));

	public static final XmlTypeSimple LOCALDATE = new XmlTypeSimple(new QName(XS, "date"), JvmModelLoader.GLOBAL.get(LocalDate.class.getName()));
	public static final XmlTypeSimple LOCALDATETIME = new XmlTypeSimple(new QName(XS, "dateTime"), JvmModelLoader.GLOBAL.get(LocalDateTime.class.getName()));
	public static final XmlTypeSimple LOCALTIME = new XmlTypeSimple(new QName(XS, "time"), JvmModelLoader.GLOBAL.get(LocalTime.class.getName()));
	public static final XmlTypeSimple OFFSETDATETIME = new XmlTypeSimple(new QName(XS, "dateTime"), JvmModelLoader.GLOBAL.get(OffsetDateTime.class.getName()));
	public static final XmlTypeSimple ZONEDDATETIME = new XmlTypeSimple(new QName(XS, "dateTime"), JvmModelLoader.GLOBAL.get(ZonedDateTime.class.getName()));
	public static final XmlTypeSimple DURATION = new XmlTypeSimple(new QName(XS, "duration"), JvmModelLoader.GLOBAL.get(Duration.class.getName()));
	public static final XmlTypeSimple PERIOD = new XmlTypeSimple(new QName(XS, "duration"), JvmModelLoader.GLOBAL.get(Period.class.getName()));

	private static final List<XmlType> BUILTIN = Arrays.asList(ANY, BOOLEAN, BOOLEAN, BYTE, BYTE, SHORT, SHORT, INT, INT, LONG, LONG, FLOAT, FLOAT, DOUBLE, DOUBLE, CHAR, CHAR,
			STRING, BIGINT, BIGDEC, LOCALDATE, LOCALDATETIME, LOCALTIME, OFFSETDATETIME, ZONEDDATETIME, DURATION);

	private final Map<String, XmlType> types = new HashMap<>();

	public XmlLoader() {
		for (XmlType t : BUILTIN)
			types.put(t.type().name(), t);
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
			throw new IllegalArgumentException("Unsupported primitive " + type);
		if (type.isEnum()) {
			XmlType t = type.annotation(jakarta.xml.bind.annotation.XmlEnum.class).flatMap(v -> v.value()).map(v -> add(v.asClass())).orElse(XmlLoader.STRING);
			if (!(t instanceof XmlTypeSimple))
				throw new IllegalArgumentException("Enum type should be a simple type " + type);
			return new XmlEnum(qname(type), type.asEnum(), (XmlTypeSimple) t);
		}
		if (type.isArray())
			return new XmlCollection(type, add(type.asArray().type()));

		ClassModel cl = type.asClass();
		if (cl.isBoxedPrimitive()) // should not happen
			throw new IllegalArgumentException("Unsupported boxed primitive " + type);
		if (cl.isAssignableTo(String.class))
			return STRING;

		if (cl.isAssignableTo(Duration.class.getName()))
			return new XmlTypeSimple(new QName(XS, "duration"), cl);
		if (cl.isAssignableTo(LocalDate.class))
			return new XmlTypeSimple(new QName(XS, "date"), cl);
		if (cl.isAssignableTo(LocalTime.class) || cl.isAssignableTo(OffsetTime.class))
			return new XmlTypeSimple(new QName(XS, "time"), cl);
		if (cl.isAssignableTo(LocalDateTime.class) || cl.isAssignableTo(OffsetDateTime.class) || cl.isAssignableTo(ZonedDateTime.class))
			return new XmlTypeSimple(new QName(XS, "dateTime"), cl);

		ClassModel col = cl.ancestor(Collection.class.getName());
		if (col != null)
			return new XmlCollection(type, add(col.parameter(0).type()));

		return createObject(cl);
	}

	private XmlType createObject(ClassModel c) {
		XmlAccessType type = c.annotation(XmlAccessorType.class).flatMap(a -> a.value()).map(a -> XmlAccessType.valueOf(a.asLiteral())).orElse(XmlAccessType.PUBLIC_MEMBER);

		String defaultNs = "";
		Optional<AnnotationModel> o = c.parent().annotation(XmlSchema.class);
		if (o.isPresent() && o.flatMap(v -> v.member("elementFormDefault")).map(v -> v.asLiteral().equals("QUALIFIED")).orElse(false))
			defaultNs = o.flatMap(v -> v.member(NAMESPACE)).map(v -> v.asLiteral()).orElse("");

		List<XmlElement> attrs = new ArrayList<>();
		List<XmlElements> elems = new ArrayList<>();
		XmlElement value = null;
		for (BeanProperty b : BeanProperty.properties(c)) {
			Optional<AnnotationModel> choice = b.annotation(jakarta.xml.bind.annotation.XmlElements.class);
			Optional<AnnotationModel> elem = b.annotation(jakarta.xml.bind.annotation.XmlElement.class);
			Optional<AnnotationModel> attr = b.annotation(jakarta.xml.bind.annotation.XmlAttribute.class);
			Optional<AnnotationModel> v = b.annotation(XmlValue.class);

			if (choice.isEmpty() && elem.isEmpty() && attr.isEmpty() && v.isEmpty()) // TODO XmlAccess
				continue;

			TypeModel t = b.type();
			if (v.isPresent()) {
				if (value != null)
					throw new IllegalArgumentException("multiple value for '" + c.name() + "'");
				if (!(add(t) instanceof XmlTypeSimple))
					throw new IllegalArgumentException("only simple type allowed in value '" + c.name() + "'");
				value = new XmlElement(this, null, b.type(), b);
			} else if (attr.isPresent()) {
				if (!(add(t) instanceof XmlTypeSimple))
					throw new IllegalArgumentException("only simple type allowed in attribute in '" + c.name() + "'");
				String n = attr.flatMap(a -> a.member("name")).map(a -> a.asLiteral()).map(i -> DEFAULT.equals(i) ? null : i).orElse(b.name());
				String ns = attr.flatMap(a -> a.member(NAMESPACE)).map(a -> a.asLiteral()).map(i -> DEFAULT.equals(i) ? null : i).orElse("");
				attrs.add(new XmlElement(this, new QName(ns, n), b.type(), b));
			} else if (choice.isPresent()) {
				AnnotationModel[] e = choice.get().value().map(a -> a.asArrayAnnotation()).orElse(null);
				if (e == null || e.length == 0)
					throw new IllegalArgumentException("Emtpy choice for " + b);

				List<XmlElements> list = new ArrayList<>();
				for (int i = 0; i < e.length; i++)
					list.add(getElems(Optional.of(e[i]), b.name(), defaultNs, b));
				elems.add(new XmlGroupElements(XmlGroup.CHOICE, list));
			} else
				elems.add(getElems(elem, b.name(), defaultNs, b));
		}

		if (value != null && !elems.isEmpty())
			throw new IllegalArgumentException("Mixed content not supported in " + c);

		XmlElements elements;
		XmlAccessOrder defaultOrder = c.annotation(XmlAccessorOrder.class).flatMap(a -> a.value()).map(a -> a.asLiteral()).map(a -> XmlAccessOrder.valueOf(a))
				.orElse(XmlAccessOrder.UNDEFINED);
		List<String> propOrder = c.annotation(jakarta.xml.bind.annotation.XmlType.class).flatMap(a -> a.member("propOrder")).map(a -> a.asArrayLiteral()).map(Arrays::asList)
				.orElse(Collections.emptyList());
		if (propOrder.isEmpty()) {
			if (defaultOrder == XmlAccessOrder.ALPHABETICAL)
				Collections.sort(elems, (a, b) -> a.firstName().compareTo(b.firstName()));
			elements = new XmlGroupElements(XmlGroup.ALL, elems);
		} else {
			Collections.sort(elems, (a, b) -> {
				String an = a.firstName();
				String bn = b.firstName();
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
			elements = new XmlGroupElements(XmlGroup.SEQUENCE, elems);
		}

		Optional<AnnotationModel> a = c.annotation(jakarta.xml.bind.annotation.XmlType.class);
		Factory f = new Factory(
				a.flatMap(v -> v.member("factoryClass")).map(v -> v.asClass().asClass())
						.filter(v -> !v.isAssignableTo(jakarta.xml.bind.annotation.XmlType.DEFAULT.class.getName())).orElse(c),
				a.flatMap(v -> v.member("factoryMethod")).map(v -> v.asLiteral()).orElse(""));
		return new XmlTypeComplex(qname(c), c, f, attrs, elements, value);
	}

	public XmlSimpleElements getElems(Optional<AnnotationModel> elem, String name, String defaultNs, BeanProperty b) {
		String n = elem.flatMap(a -> a.member("name")).map(a -> a.asLiteral()).map(i -> DEFAULT.equals(i) ? null : i).orElse(name);
		String ns = elem.flatMap(a -> a.member(NAMESPACE)).map(a -> a.asLiteral()).map(i -> DEFAULT.equals(i) ? null : i).orElse(defaultNs);
		TypeModel type = elem.flatMap(a -> a.member("type")).map(a -> a.asClass())
				.filter(a -> !a.name().equals(jakarta.xml.bind.annotation.XmlElement.DEFAULT.class.getName())).orElse(b.type());
		return new XmlSimpleElements(new XmlElement(this, new QName(ns, n), type, b));
	}

	private static QName qname(TypeModel type) {
		Optional<AnnotationModel> a = type.annotation(jakarta.xml.bind.annotation.XmlType.class);

		String name = type.simpleName();
		String ns = type.parent().annotation(XmlSchema.class).flatMap(v -> v.member(NAMESPACE)).map(v -> v.asLiteral()).orElse("");
		if (a.isPresent()) {
			name = a.flatMap(v -> v.member("name")).map(v -> v.asLiteral()).filter(v -> !v.equals(DEFAULT)).orElse(name);
			ns = a.flatMap(v -> v.member(NAMESPACE)).map(v -> v.asLiteral()).filter(v -> !v.equals(DEFAULT)).orElse(ns);
		}
		return new QName(ns, name);
	}
}