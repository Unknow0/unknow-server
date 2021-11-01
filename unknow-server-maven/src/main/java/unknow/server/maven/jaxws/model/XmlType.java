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
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.type.Type;

import unknow.server.maven.TypeCache;
import unknow.server.maven.jaxws.model.XmlObject.Factory;
import unknow.server.maven.jaxws.model.XmlObject.XmlElem;
import unknow.server.maven.jaxws.model.XmlObject.XmlField;

/**
 * @author unknow
 */
public interface XmlType {

	/**
	 * generate an expression to convert from value (string for simple type, Object for complex)
	 * 
	 * @param types
	 * @param v     string expression
	 * @return and expression to the right type
	 */
	Expression convert(TypeCache types, Expression v);

	/**
	 * generate an expression to value toString
	 * 
	 * @param types
	 * @param v     string expression
	 * @return and expression to the right type
	 */
	default Expression toString(@SuppressWarnings("unused") TypeCache types, Expression v) {
		return v;
	}

	/**
	 * generate the binaryName of the underling java type
	 * 
	 * @return binary name
	 */
	String binaryName();

	/**
	 * @return true if this type is simple
	 */
	default boolean isSimple() {
		return true;
	}

	public static final XmlType XmlString = new XmlType() {
		@Override
		public Expression convert(TypeCache types, Expression v) {
			return v;
		}

		@Override
		public String binaryName() {
			return "java.lang.String";
		}

		@Override
		public String toString() {
			return "string";
		}
	};
	public static final XmlType XmlBigDecimal = new XmlType() {
		@Override
		public Expression convert(TypeCache types, Expression v) {
			return new ObjectCreationExpr(null, types.get(BigDecimal.class), new NodeList<>(v));
		}

		@Override
		public Expression toString(TypeCache types, Expression v) {
			return new MethodCallExpr(v, "toString");
		}

		@Override
		public String binaryName() {
			return BigDecimal.class.getCanonicalName();
		}

		@Override
		public String toString() {
			return "bigDecimal";
		}
	};
	public static final XmlType XmlBigInteger = new XmlType() {
		@Override
		public Expression convert(TypeCache types, Expression v) {
			return new ObjectCreationExpr(null, types.get(BigInteger.class), new NodeList<>(v));
		}

		@Override
		public Expression toString(TypeCache types, Expression v) {
			return new MethodCallExpr(v, "toString");
		}

		@Override
		public String binaryName() {
			return BigInteger.class.getCanonicalName();
		}

		@Override
		public String toString() {
			return "bigInteger";
		}
	};
	public static final XmlType XmlBoolean = new XmlType() {
		@Override
		public Expression convert(TypeCache types, Expression v) {
			return new BinaryExpr(
					new MethodCallExpr(new StringLiteralExpr("true"), "equalsIgnoreCase", new NodeList<>(v)),
					new MethodCallExpr(new StringLiteralExpr("1"), "equals", new NodeList<>(v)),
					Operator.OR);
		}

		@Override
		public String binaryName() {
			return "java.lang.Boolean";
		}

		@Override
		public String toString() {
			return "XmlString";
		}
	};

	public static final XmlType XmlByte = new XmlPrimitive(byte.class, Integer.class, "parseInt");
	public static final XmlType XmlChar = new XmlPrimitive(char.class, Integer.class, "parseInt");
	public static final XmlType XmlShort = new XmlPrimitive(short.class, Integer.class, "parseInt");
	public static final XmlType XmlInt = new XmlPrimitive(int.class, Integer.class, "parseInt");
	public static final XmlType XmlLong = new XmlPrimitive(long.class, Long.class, "parseLong");
	public static final XmlType XmlFloat = new XmlPrimitive(float.class, Float.class, "parseFloat");
	public static final XmlType XmlDouble = new XmlPrimitive(double.class, Double.class, "parseDouble");

	public static class XmlPrimitive implements XmlType {
		private final Class<?> clazz;
		private final Class<?> parser;
		private final String parse;

		/**
		 * create new XmlType.XmlPrimitive
		 */
		public XmlPrimitive(Class<?> clazz, Class<?> parser, String parse) {
			this.clazz = clazz;
			this.parser = parser;
			this.parse = parse;
		}

		@Override
		public Expression convert(TypeCache types, Expression v) {
			Expression e = new MethodCallExpr(new TypeExpr(types.get(parser)), parse, new NodeList<>(v));
			if (parser == Integer.class && clazz != int.class)
				e = new CastExpr(types.get(clazz), e);
			return e;
		}

		@Override
		public Expression toString(TypeCache types, Expression v) {
			return new MethodCallExpr(new TypeExpr(types.get(parser)), "toString", new NodeList<>(v));
		}

		@Override
		public String binaryName() {
			return clazz.getName();
		}

		@Override
		public String toString() {
			return clazz.getName();
		}
	}

	public static class XmlList implements XmlType {
		private final XmlType component;

		public XmlList(XmlType component) {
			this.component = component;
		}

		@Override
		public Expression convert(TypeCache types, Expression v) {
			return component.convert(types, v);
		}

		@Override
		public Expression toString(TypeCache types, Expression v) {
			return component.toString(types, v);
		}

		@Override
		public String binaryName() {
			return "[" + component.binaryName();
		}

		@Override
		public String toString() {
			return "XmlList {" + component + "}";
		}
	}

	public static XmlType get(Type type, Map<String, ClassOrInterfaceDeclaration> classes) {
		if (type.isArrayType())
			return new XmlList(get(type.asArrayType().getComponentType(), classes));
		if (type.isPrimitiveType()) {
			switch (type.asPrimitiveType().getType()) {
				case BOOLEAN:
					return XmlBoolean;
				case BYTE:
					return XmlByte;
				case CHAR:
					return XmlChar;
				case SHORT:
					return XmlShort;
				case INT:
					return XmlInt;
				case LONG:
					return XmlLong;
				case FLOAT:
					return XmlFloat;
				case DOUBLE:
					return XmlDouble;
				default:
					return null;
			}
		}
		if (!type.isClassOrInterfaceType())
			throw new RuntimeException("unknown type: '" + type + "'");

		String describe = type.asClassOrInterfaceType().resolve().describe();
		if ("java.lang.String".equals(describe))
			return XmlString;
		if ("java.lang.Boolean".equals(describe))
			return XmlBoolean;
		if ("java.lang.Byte".equals(describe))
			return XmlByte;
		if ("java.lang.Character".equals(describe))
			return XmlChar;
		if ("java.lang.Short".equals(describe))
			return XmlShort;
		if ("java.lang.Integer".equals(describe))
			return XmlInt;
		if ("java.lang.Long".equals(describe))
			return XmlLong;
		if ("java.lang.Float".equals(describe))
			return XmlFloat;
		if ("java.lang.Double".equals(describe))
			return XmlDouble;
		if (BigInteger.class.getCanonicalName().equals(describe))
			return XmlBigInteger;
		if (BigDecimal.class.getCanonicalName().equals(describe))
			return XmlBigDecimal;

		// TODO Collection
		// TODO Map ?

		ClassOrInterfaceDeclaration c = classes.get(describe);
		if (c != null)
			return loadObject(c, classes);
		// TODO load from class
		return null;
	}

	public static XmlObject loadObject(ClassOrInterfaceDeclaration c, Map<String, ClassOrInterfaceDeclaration> classes) {
		String namespace = "";

		Optional<AnnotationExpr> a = c.getAnnotationByClass(XmlAccessorType.class);
		XmlAccessType type = XmlAccessType.PUBLIC_MEMBER;
		if (a.isPresent()) {
			AnnotationExpr an = a.get();
			Optional<SingleMemberAnnotationExpr> v = an.findFirst(SingleMemberAnnotationExpr.class);
			Expression e = v.isPresent() ? v.get().getMemberValue() : an.findFirst(MemberValuePair.class, m -> "value".equals(m.getNameAsString())).get().getValue();
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
			namespace = getNs(a, namespace);
		}
		a = c.getAnnotationByClass(javax.xml.bind.annotation.XmlRootElement.class);
		if (a.isPresent())
			namespace = getNs(a, namespace);

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
			if (annot != null || type == XmlAccessType.FIELD || type == XmlAccessType.PUBLIC_MEMBER && m.hasModifier(Keyword.PUBLIC)) {
				String setter = "set" + m.getNameAsString().substring(3);

				Optional<MethodDeclaration> s = c.getMethods().stream().filter(e -> e.getNameAsString().equals(setter)).filter(e -> e.getParameters().size() == 1 && e.getParameter(0).getType().resolve().describe().equals(m.getType().resolve().describe())).findFirst();
				if (!s.isPresent())
					throw new RuntimeException("missing setter for '" + n + "' field in '" + c.getNameAsString() + "' class");

				XmlType t = XmlType.get(d.v.getType(), classes);
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

		return new XmlObject(c.resolve().getQualifiedName(), factory, attrs, elems, value);
	}

	static String getName(Optional<AnnotationExpr> a, String def) {
		if (!a.isPresent())
			return def;
		return a.get().findFirst(MemberValuePair.class, m -> "name".equals(m.getNameAsString())).map(e -> e.getValue().asStringLiteralExpr().getValue()).map(v -> "##default".equals(v) ? def : v).orElse(def);
	}

	static String getNs(Optional<AnnotationExpr> a, String def) {
		if (!a.isPresent())
			return def;
		return a.get().findFirst(MemberValuePair.class, m -> "namespace".equals(m.getNameAsString())).map(e -> e.getValue().asStringLiteralExpr().getValue()).map(v -> "##default".equals(v) ? def : v).orElse(def);
	}

}
