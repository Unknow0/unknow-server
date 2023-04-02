/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import com.github.javaparser.ast.expr.Expression;

import unknow.server.maven.TypeCache;
import unknow.server.maven.model.TypeModel;

/**
 * an xmlType binding
 * 
 * @author unknow
 */
public class XmlType<T extends TypeModel> {
	private final T javaType;
	private final String ns;
	private final String name;

	public XmlType(T type, String ns, String name) {
		this.javaType = type;
		this.name = name;
		this.ns = ns;
	}

	public String qname() {
		return ns.isEmpty() ? name : "{" + ns + "}" + name;
	}

	public String ns() {
		return ns;
	}

	public String name() {
		return name;
	}

	public T javaType() {
		return javaType;
	}

	@Override
	public String toString() {
		return qname();
	}

	/**
	 * generate an expression to convert from value (string for simple type, Object for complex)
	 * 
	 * @param types
	 * @param v     string expression
	 * @return and expression to the right type
	 */
	public Expression fromString(@SuppressWarnings("unused") TypeCache types, Expression v) {
		return v;
	}

	/**
	 * generate an expression to convert value toString
	 * 
	 * @param types
	 * @param v     value
	 * @return the value as a string
	 */
	public Expression toString(@SuppressWarnings("unused") TypeCache types, Expression v) {
		return v;
	}

	/**
	 * @return true if this type is simple
	 */
	public boolean isSimple() {
		return true;
	}

//	public static final XmlType XmlBoolean = new XmlType() {
//		private final SchemaData schema = new SchemaData("boolean", "http://www.w3.org/2001/XMLSchema", null, null);
//
//		@Override
//		public Expression convert(TypeCache types, Expression v) {
//			return new BinaryExpr(new MethodCallExpr(new StringLiteralExpr("true"), "equalsIgnoreCase", new NodeList<>(v)),
//					new MethodCallExpr(new StringLiteralExpr("1"), "equals", new NodeList<>(v)), Operator.OR);
//		}
//
//		@Override
//		public String clazz() {
//			return "java.lang.Boolean";
//		}
//
//		@Override
//		public SchemaData schema() {
//			return schema;
//		}
//
//		@Override
//		public String toString() {
//			return "XmlString";
//		}
//	};
//
//	public static final XmlType XmlByte = new XmlTypePrimitive(byte.class, Integer.class, "parseInt", "byte");
//	public static final XmlType XmlChar = new XmlTypePrimitive(char.class, Integer.class, "parseInt", "int");
//	public static final XmlType XmlShort = new XmlTypePrimitive(short.class, Integer.class, "parseInt", "short");
//	public static final XmlType XmlInt = new XmlTypePrimitive(int.class, Integer.class, "parseInt", "int");
//	public static final XmlType XmlLong = new XmlTypePrimitive(long.class, Long.class, "parseLong", "long");
//	public static final XmlType XmlFloat = new XmlTypePrimitive(float.class, Float.class, "parseFloat", "decimal");
//	public static final XmlType XmlDouble = new XmlTypePrimitive(double.class, Double.class, "parseDouble", "decimal");
//
//	public static class XmlList implements XmlType {
//		public final XmlType component;
//
//		public XmlList(XmlType component) {
//			this.component = component;
//		}
//
//		@Override
//		public Expression convert(TypeCache types, Expression v) {
//			return component.convert(types, v);
//		}
//
//		@Override
//		public Expression toString(TypeCache types, Expression v) {
//			return component.toString(types, v);
//		}
//
//		@Override
//		public String clazz() {
//			return "java.util.List";
//		}
//
//		@Override
//		public String binaryName() {
//			return "[" + component.binaryName();
//		}
//
//		@Override
//		public SchemaData schema() {
//			return component.schema();
//		}
//
//		@Override
//		public String toString() {
//			return "XmlList {" + component + "}";
//		}
//	}
}
