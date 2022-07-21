/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import unknow.server.maven.TypeCache;

/**
 * an xmlType binding
 * 
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
	 * @return the class name
	 */
	String clazz();

	/**
	 * generate the binaryName of the underling java type
	 * 
	 * @return binary name
	 */
	default String binaryName() {
		return clazz() + ';';
	}

	/**
	 * get the schema data
	 * 
	 * @return the schema
	 */
	SchemaData schema();

	/**
	 * @return true if java type is primitive
	 */
	default boolean isPrimitive() {
		return false;
	}

	/**
	 * @return true if this type is simple
	 */
	default boolean isSimple() {
		return true;
	}

	public static final XmlType XmlString = new XmlType() {
		private final SchemaData schema = new SchemaData("string", "http://www.w3.org/2001/XMLSchema", null, null);

		@Override
		public Expression convert(TypeCache types, Expression v) {
			return v;
		}

		@Override
		public String clazz() {
			return "java.lang.String";
		}

		@Override
		public SchemaData schema() {
			return schema;
		}

		@Override
		public String toString() {
			return "string";
		}
	};
	public static final XmlType XmlBigDecimal = new XmlType() {
		private final SchemaData schema = new SchemaData("decimal", "http://www.w3.org/2001/XMLSchema", null, null);

		@Override
		public Expression convert(TypeCache types, Expression v) {
			return new ObjectCreationExpr(null, types.get(BigDecimal.class), new NodeList<>(v));
		}

		@Override
		public Expression toString(TypeCache types, Expression v) {
			return new MethodCallExpr(v, "toString");
		}

		@Override
		public String clazz() {
			return BigDecimal.class.getName();
		}

		@Override
		public SchemaData schema() {
			return schema;
		}

		@Override
		public String toString() {
			return "bigDecimal";
		}
	};
	public static final XmlType XmlBigInteger = new XmlType() {
		private final SchemaData schema = new SchemaData("integer", "http://www.w3.org/2001/XMLSchema", null, null);

		@Override
		public Expression convert(TypeCache types, Expression v) {
			return new ObjectCreationExpr(null, types.get(BigInteger.class), new NodeList<>(v));
		}

		@Override
		public Expression toString(TypeCache types, Expression v) {
			return new MethodCallExpr(v, "toString");
		}

		@Override
		public String clazz() {
			return BigInteger.class.getName();
		}

		@Override
		public SchemaData schema() {
			return schema;
		}

		@Override
		public String toString() {
			return "bigInteger";
		}
	};
	public static final XmlType XmlBoolean = new XmlType() {
		private final SchemaData schema = new SchemaData("boolean", "http://www.w3.org/2001/XMLSchema", null, null);

		@Override
		public Expression convert(TypeCache types, Expression v) {
			return new BinaryExpr(new MethodCallExpr(new StringLiteralExpr("true"), "equalsIgnoreCase", new NodeList<>(v)),
					new MethodCallExpr(new StringLiteralExpr("1"), "equals", new NodeList<>(v)), Operator.OR);
		}

		@Override
		public String clazz() {
			return "java.lang.Boolean";
		}

		@Override
		public SchemaData schema() {
			return schema;
		}

		@Override
		public String toString() {
			return "XmlString";
		}
	};

	public static final XmlType XmlByte = new XmlTypePrimitive(byte.class, Integer.class, "parseInt", "byte");
	public static final XmlType XmlChar = new XmlTypePrimitive(char.class, Integer.class, "parseInt", "int");
	public static final XmlType XmlShort = new XmlTypePrimitive(short.class, Integer.class, "parseInt", "short");
	public static final XmlType XmlInt = new XmlTypePrimitive(int.class, Integer.class, "parseInt", "int");
	public static final XmlType XmlLong = new XmlTypePrimitive(long.class, Long.class, "parseLong", "long");
	public static final XmlType XmlFloat = new XmlTypePrimitive(float.class, Float.class, "parseFloat", "decimal");
	public static final XmlType XmlDouble = new XmlTypePrimitive(double.class, Double.class, "parseDouble", "decimal");

	public static class XmlList implements XmlType {
		public final XmlType component;

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
		public String clazz() {
			return "java.util.List";
		}

		@Override
		public String binaryName() {
			return "[" + component.binaryName();
		}

		@Override
		public SchemaData schema() {
			return component.schema();
		}

		@Override
		public String toString() {
			return "XmlList {" + component + "}";
		}
	}
}
