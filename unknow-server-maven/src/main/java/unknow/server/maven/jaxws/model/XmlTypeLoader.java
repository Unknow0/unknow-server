/**
 * 
 */
package unknow.server.maven.jaxws.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import unknow.server.maven.jaxws.model.XmlEnum.XmlEnumEntry;
import unknow.server.maven.jaxws.model.XmlObject.Factory;
import unknow.server.maven.jaxws.model.XmlObject.XmlElem;
import unknow.server.maven.jaxws.model.XmlObject.XmlField;
import unknow.server.maven.jaxws.model.XmlType.XmlList;

/**
 * load XmlType
 * 
 * @author unknow
 */
public class XmlTypeLoader {
	private static final Map<String, XmlType> DEFAULT = new HashMap<>();
	static {
		DEFAULT.put("java.lang.String", XmlType.XmlString);
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

	final Map<String, TypeDeclaration<?>> classes;
	private final Map<String, XmlType> loaded;

	/**
	 * create new XmlTypeLoader
	 * 
	 * @param classes all known sources class
	 */
	public XmlTypeLoader(Map<String, TypeDeclaration<?>> classes) {
		this.classes = classes;
		this.loaded = new HashMap<>(DEFAULT);
	}

	/**
	 * get an XmlType
	 * 
	 * @param type type to convert
	 * @return the XmlType
	 */
	public XmlType get(Type type) {
		ResolvedType r = type.resolve();
		String describe = r.describe();
		XmlType xmlType = loaded.get(describe);
		if (xmlType == null) {
			XmlTypeWrapper w = new XmlTypeWrapper();
			loaded.put(describe, w);
			xmlType = create(type);
			w.setDelegate(xmlType);
			loaded.put(describe, xmlType);
		}
		return xmlType;
	}

	private XmlType create(Type type) {
		if (type.isArrayType())
			return new XmlList(get(type.asArrayType().getComponentType()));
		if (type.isPrimitiveType()) {
			switch (type.asPrimitiveType().getType()) {
				case BOOLEAN:
					return XmlType.XmlBoolean;
				case BYTE:
					return XmlType.XmlByte;
				case CHAR:
					return XmlType.XmlChar;
				case SHORT:
					return XmlType.XmlShort;
				case INT:
					return XmlType.XmlInt;
				case LONG:
					return XmlType.XmlLong;
				case FLOAT:
					return XmlType.XmlFloat;
				case DOUBLE:
					return XmlType.XmlDouble;
				default:
					return null;
			}
		}
		if (!type.isClassOrInterfaceType())
			throw new RuntimeException("unknown type: '" + type + "'");
		ResolvedReferenceType resolve = type.asClassOrInterfaceType().resolve();
		String describe = resolve.describe();
		TypeDeclaration<?> c = classes.get(describe);
		if (c == null)
			// TODO class.forName & create
			throw new RuntimeException("missing class '" + describe + "'");

		if (c.isEnumDeclaration()) // TODO XmlEnum/XmlEnumValue
			return createEnum(c.asEnumDeclaration());
		if (c.isClassOrInterfaceDeclaration()) {
			// TODO Collection
			// TODO Map ?

			return createObject(c.asClassOrInterfaceDeclaration());
		}
		throw new RuntimeException("invalid type declaration " + c);
	}

	/**
	 * @param enumDeclaration
	 * @return
	 */
	private XmlType createEnum(EnumDeclaration type) {
		List<XmlEnumEntry> entries = new ArrayList<>();
		for (EnumConstantDeclaration e : type.getEntries()) {
			String v = getValue(e.getAnnotationByClass(XmlEnumValue.class), e.getNameAsString());
			entries.add(new XmlEnumEntry(e.getNameAsString(), v));
		}
		return new XmlEnum(type.resolve().getQualifiedName(), entries, getSchema(type), "enum" + loaded.size() + "$");
	}

	private XmlObject createObject(ClassOrInterfaceDeclaration c) {
		// TODO get namespace data ?
		String namespace = "";
		String name = c.getNameAsString();

		Optional<AnnotationExpr> a = c.getAnnotationByClass(XmlAccessorType.class);
		XmlAccessType type = XmlAccessType.PUBLIC_MEMBER;
		if (a.isPresent()) {
			AnnotationExpr an = a.get();
			Optional<SingleMemberAnnotationExpr> v = an.findFirst(SingleMemberAnnotationExpr.class);
			Expression e = v.isPresent() ? v.get().getMemberValue()
					: an.findFirst(MemberValuePair.class, m -> "value".equals(m.getNameAsString())).map(m -> m.getValue()).orElse(null);
			if (e != null)
				type = XmlAccessType.valueOf(e.asFieldAccessExpr().getNameAsString());
		}

		Factory factory = null;
		a = c.getAnnotationByClass(javax.xml.bind.annotation.XmlType.class);
		List<String> propOrder = null;
		if (a.isPresent()) {
			String cl = a.get().findFirst(MemberValuePair.class, m -> "factoryClass".equals(m.getNameAsString()))
					.map(v -> v.getValue().asClassExpr().getType().resolve().describe()).orElse(null);
			String method = a.get().findFirst(MemberValuePair.class, m -> "factoryMethod".equals(m.getNameAsString())).map(v -> v.getValue().asStringLiteralExpr().asString())
					.orElse("");

			if (!method.isEmpty()) {
				if (javax.xml.bind.annotation.XmlType.DEFAULT.class.getCanonicalName().equals(cl))
					factory = new Factory(c.resolve().getQualifiedName(), method);
				else
					factory = new Factory(cl, method);
			}
			name = getName(a, name);
			namespace = getNs(a, namespace);
			Expression e = a.get().findFirst(MemberValuePair.class, m -> "propOrder".equals(m.getNameAsString())).map(m -> m.getValue()).orElse(null);
			if (e != null) {
				if (e.isStringLiteralExpr())
					propOrder = Arrays.asList(e.asStringLiteralExpr().asString());
				else if (e.isArrayInitializerExpr())
					propOrder = e.asArrayInitializerExpr().getValues().stream().map(m -> m.asStringLiteralExpr().asString()).collect(Collectors.toList());
			}
		}

		class D {
			final FieldDeclaration f;

			public D(FieldDeclaration f) {
				this.f = f;
			}
		}
		Map<String, D> fields = new HashMap<>();

		for (FieldDeclaration f : c.getFields()) {
			if (f.getModifiers().contains(Modifier.transientModifier()) || f.getAnnotationByClass(XmlTransient.class).isPresent())
				continue;

			Optional<AnnotationExpr> eleme = f.getAnnotationByClass(XmlElement.class);
			Optional<AnnotationExpr> attri = f.getAnnotationByClass(XmlAttribute.class);
			Optional<AnnotationExpr> value = f.getAnnotationByClass(XmlValue.class);

			AnnotationExpr annot = eleme.orElse(attri.orElse(value.orElse(null)));
			if (annot != null || type == XmlAccessType.FIELD || type == XmlAccessType.PUBLIC_MEMBER && f.hasModifier(Keyword.PUBLIC)) {
				for (VariableDeclarator v : f.getVariables()) {
					String setter = "set" + Character.toUpperCase(v.getNameAsString().charAt(0)) + v.getNameAsString().substring(1);
					String getter = "get" + Character.toUpperCase(v.getNameAsString().charAt(0)) + v.getNameAsString().substring(1);
					// TODO check also is* for boolean

					Optional<MethodDeclaration> s = c.getMethods().stream().filter(m -> m.getNameAsString().equals(setter))
							.filter(m -> m.getParameters().size() == 1 && m.getParameter(0).getType().resolve().describe().equals(v.getType().resolve().describe()))
							.findFirst();
					Optional<MethodDeclaration> g = c.getMethods().stream().filter(m -> m.getNameAsString().equals(getter)).filter(m -> m.getParameters().size() == 0)
							.findFirst();
					if (!s.isPresent())
						throw new RuntimeException("missing setter for '" + v.getNameAsString() + "' field in '" + c.getNameAsString() + "' class");
					if (!g.isPresent()) // TODO check is* if boolean
						throw new RuntimeException("missing setter for '" + v.getNameAsString() + "' field in '" + c.getNameAsString() + "' class");

					fields.put(v.getNameAsString(), new D(f));
				}
			}
		}

		List<XmlField> attrs = new ArrayList<>();
		List<XmlField> elems = new ArrayList<>();
		XmlElem value = null;
		for (MethodDeclaration m : c.getMethods()) {
			// skip non getter
			if (m.getParameters().size() > 0 || !m.getNameAsString().startsWith("get"))
				continue;
			String n = Character.toLowerCase(m.getNameAsString().charAt(3)) + m.getNameAsString().substring(4);

			Optional<AnnotationExpr> eleme = m.getAnnotationByClass(XmlElement.class);
			Optional<AnnotationExpr> attri = m.getAnnotationByClass(XmlAttribute.class);
			Optional<AnnotationExpr> v = m.getAnnotationByClass(XmlValue.class);

			boolean hasAnnotation = eleme.isPresent() || attri.isPresent() || v.isPresent();
			D d = fields.get(n);
			if (d != null && !hasAnnotation) {
				eleme = d.f.getAnnotationByClass(XmlElement.class);
				attri = d.f.getAnnotationByClass(XmlAttribute.class);
				v = d.f.getAnnotationByClass(XmlValue.class);
			}

			AnnotationExpr annot = eleme.orElse(attri.orElse(v.orElse(null)));
			if (annot != null || type == XmlAccessType.FIELD && d != null || type == XmlAccessType.PUBLIC_MEMBER && m.hasModifier(Keyword.PUBLIC)) {
				String setter = "set" + m.getNameAsString().substring(3);

				Optional<MethodDeclaration> s = c.getMethods().stream().filter(e -> e.getNameAsString().equals(setter))
						.filter(e -> e.getParameters().size() == 1 && e.getParameter(0).getType().resolve().describe().equals(m.getType().resolve().describe())).findFirst();
				if (!s.isPresent())
					throw new RuntimeException("missing setter for '" + n + "' field in '" + c.getNameAsString() + "' class");
				XmlType t = get(m.getType());
				if (v.isPresent()) {
					if (value != null)
						throw new RuntimeException("multiple value for '" + c.getNameAsString() + "'");
					value = new XmlElem(t, m.getNameAsString(), setter);
				} else if (attri.isPresent()) {
					if (!t.isSimple())
						throw new RuntimeException("only simple type allowed in attribute in '" + c.getNameAsString() + "'");
					n = getName(attri, n);
					String ns = getNs(attri, "");
					attrs.add(new XmlField(t, m.getNameAsString(), setter, n, ns));
				} else {
					n = getName(eleme, n);
					String ns = getNs(eleme, namespace);
					elems.add(new XmlField(t, m.getNameAsString(), setter, n, ns));
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
		return new XmlObject(c.resolve().getQualifiedName(), factory, attrs, elems, value, getSchema(c));
	}

	private static String getValue(Optional<AnnotationExpr> a, String def) {
		if (!a.isPresent())
			return def;

		Expression e = null;
		if (a.get().isSingleMemberAnnotationExpr())
			e = a.get().asSingleMemberAnnotationExpr().getMemberValue();
		else {
			for (Node n : a.get().getChildNodes()) {
				MemberValuePair m = (MemberValuePair) n;
				if ("value".equals(m.getNameAsString())) {
					e = m.getValue();
					break;
				}
			}
		}
		if (e == null)
			return def;
		if (e.isStringLiteralExpr())
			return e.asStringLiteralExpr().asString();
		return def;
	}

	/**
	 * @param c
	 * @return
	 */
	private static SchemaData getSchema(TypeDeclaration<?> c) {
		// TODO get namespace data ?
		String ns = "";
		String name = c.getNameAsString();

		String rootElem = null;
		String rootNs = null;
		Optional<AnnotationExpr> a = c.getAnnotationByClass(javax.xml.bind.annotation.XmlRootElement.class);
		if (a.isPresent()) {
			rootElem = getName(a, name);
			rootNs = getNs(a, ns);
		}

		a = c.getAnnotationByClass(javax.xml.bind.annotation.XmlType.class);
		if (a.isPresent()) {
			name = getName(a, name);
			ns = getNs(a, ns);
		}

		return new SchemaData(name, ns, rootElem, rootNs);
	}

	private static String getName(Optional<AnnotationExpr> a, String def) {
		if (!a.isPresent())
			return def;
		return a.get().findFirst(MemberValuePair.class, m -> "name".equals(m.getNameAsString())).map(e -> e.getValue().asStringLiteralExpr().getValue())
				.map(v -> "##default".equals(v) ? def : v).orElse(def);
	}

	private static String getNs(Optional<AnnotationExpr> a, String def) {
		if (!a.isPresent())
			return def;
		return a.get().findFirst(MemberValuePair.class, m -> "namespace".equals(m.getNameAsString())).map(e -> e.getValue().asStringLiteralExpr().getValue())
				.map(v -> "##default".equals(v) ? def : v).orElse(def);
	}
}
