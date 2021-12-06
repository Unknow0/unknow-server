/**
 * 
 */
package unknow.server.maven.jaxws.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

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
	final Map<String, ClassOrInterfaceDeclaration> classes;
	private final Map<String, XmlType> loaded;

	/**
	 * create new XmlTypeLoader
	 * 
	 * @param classes all known sources class
	 */
	public XmlTypeLoader(Map<String, ClassOrInterfaceDeclaration> classes) {
		this.classes = classes;
		this.loaded = new HashMap<>();
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
		if (xmlType == null)
			loaded.put(describe, xmlType = create(type));
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
		if ("java.lang.String".equals(describe))
			return XmlType.XmlString;
		if ("java.lang.Boolean".equals(describe))
			return XmlType.XmlBoolean;
		if ("java.lang.Byte".equals(describe))
			return XmlType.XmlByte;
		if ("java.lang.Character".equals(describe))
			return XmlType.XmlChar;
		if ("java.lang.Short".equals(describe))
			return XmlType.XmlShort;
		if ("java.lang.Integer".equals(describe))
			return XmlType.XmlInt;
		if ("java.lang.Long".equals(describe))
			return XmlType.XmlLong;
		if ("java.lang.Float".equals(describe))
			return XmlType.XmlFloat;
		if ("java.lang.Double".equals(describe))
			return XmlType.XmlDouble;
		if (BigInteger.class.getCanonicalName().equals(describe))
			return XmlType.XmlBigInteger;
		if (BigDecimal.class.getCanonicalName().equals(describe))
			return XmlType.XmlBigDecimal;
//		if (isEnum(resolve)) // TODO XmlEnum/XmlEnumValue
//			return new XmlType.Enum(describe);

		// TODO date/time
		// TODO Collection
		// TODO Map ?

		ClassOrInterfaceDeclaration c = classes.get(describe);
		if (c != null)
			return createObject(c);
		// TODO class.forName & create
		throw new RuntimeException("missing class '" + describe + "'");
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
			Expression e = v.isPresent() ? v.get().getMemberValue() : an.findFirst(MemberValuePair.class, m -> "value".equals(m.getNameAsString())).map(m -> m.getValue()).orElse(null);
			if (e != null)
				type = XmlAccessType.valueOf(e.asFieldAccessExpr().getNameAsString());
		}

		Factory factory = null;
		a = c.getAnnotationByClass(javax.xml.bind.annotation.XmlType.class);
		if (a.isPresent()) {
			String cl = a.get().findFirst(MemberValuePair.class, m -> "factoryClass".equals(m.getNameAsString())).map(v -> v.getValue().asClassExpr().getType().resolve().describe()).orElse(null);
			String method = a.get().findFirst(MemberValuePair.class, m -> "factoryMethod".equals(m.getNameAsString())).map(v -> v.getValue().asStringLiteralExpr().asString()).orElse("");

			if (!method.isEmpty()) {
				if (javax.xml.bind.annotation.XmlType.DEFAULT.class.getCanonicalName().equals(cl))
					factory = new Factory(c.resolve().getQualifiedName(), method);
				else
					factory = new Factory(cl, method);
			}
			// TODO prop order
			name = getName(a, name);
			namespace = getNs(a, namespace);
		}

		class D {
			final FieldDeclaration f;
			final VariableDeclarator v;

			public D(FieldDeclaration f, VariableDeclarator v) {
				this.f = f;
				this.v = v;
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

					Optional<MethodDeclaration> s = c.getMethods().stream().filter(m -> m.getNameAsString().equals(setter)).filter(m -> m.getParameters().size() == 1 && m.getParameter(0).getType().resolve().describe().equals(v.getType().resolve().describe())).findFirst();
					Optional<MethodDeclaration> g = c.getMethods().stream().filter(m -> m.getNameAsString().equals(getter)).filter(m -> m.getParameters().size() == 0).findFirst();
					if (!s.isPresent())
						throw new RuntimeException("missing setter for '" + v.getNameAsString() + "' field in '" + c.getNameAsString() + "' class");
					if (!g.isPresent()) // TODO check is* if boolean
						throw new RuntimeException("missing setter for '" + v.getNameAsString() + "' field in '" + c.getNameAsString() + "' class");

					fields.put(v.getNameAsString(), new D(f, v));
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

				Optional<MethodDeclaration> s = c.getMethods().stream().filter(e -> e.getNameAsString().equals(setter)).filter(e -> e.getParameters().size() == 1 && e.getParameter(0).getType().resolve().describe().equals(m.getType().resolve().describe())).findFirst();
				if (!s.isPresent())
					throw new RuntimeException("missing setter for '" + n + "' field in '" + c.getNameAsString() + "' class");
				if (d == null)
					; // TODO
				XmlType t = get(d.v.getType());
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

		return new XmlObject(c.resolve().getQualifiedName(), factory, attrs, elems, value, getSchema(c));
	}

	/**
	 * @param c
	 * @return
	 */
	private static SchemaData getSchema(ClassOrInterfaceDeclaration c) {
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

	private static boolean isEnum(ResolvedReferenceType t) {
		for (ResolvedReferenceType p : t.getAllAncestors()) {
			if (p.isJavaLangEnum())
				return true;
		}
		return false;
	}

	private static String getName(Optional<AnnotationExpr> a, String def) {
		if (!a.isPresent())
			return def;
		return a.get().findFirst(MemberValuePair.class, m -> "name".equals(m.getNameAsString())).map(e -> e.getValue().asStringLiteralExpr().getValue()).map(v -> "##default".equals(v) ? def : v).orElse(def);
	}

	private static String getNs(Optional<AnnotationExpr> a, String def) {
		if (!a.isPresent())
			return def;
		return a.get().findFirst(MemberValuePair.class, m -> "namespace".equals(m.getNameAsString())).map(e -> e.getValue().asStringLiteralExpr().getValue()).map(v -> "##default".equals(v) ? def : v).orElse(def);
	}
}
