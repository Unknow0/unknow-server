/**
 * 
 */
package unknow.server.maven.jaxws.model;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;

import unknow.server.maven.TypeCache;

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
}
