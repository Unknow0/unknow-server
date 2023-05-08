/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;

import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.model.TypeModel;

/**
 * @author unknow
 */
public class XmlDefaultType extends XmlType<DummyModel> {
	public static final XmlType<TypeModel> XmlString = new XmlType<>(new DummyModel(String.class.getName()), "http://www.w3.org/2001/XMLSchema", "string");
	public static final XmlDefaultType XmlBigDecimal = new XmlDefaultType(BigDecimal.class.getName(), "http://www.w3.org/2001/XMLSchema", "decimal");
	public static final XmlDefaultType XmlBigInteger = new XmlDefaultType(BigInteger.class.getName(), "http://www.w3.org/2001/XMLSchema", "integer");
	public static final XmlDefaultType XmlBoolean = new XmlDefaultType(Boolean.class.getName(), "http://www.w3.org/2001/XMLSchema", "boolean");
	public static final XmlDefaultType XmlByte = new XmlDefaultType(Byte.class.getName(), "http://www.w3.org/2001/XMLSchema", "byte");
	public static final XmlDefaultType XmlChar = new XmlDefaultType(Character.class.getName(), "http://www.w3.org/2001/XMLSchema", "string");
	public static final XmlDefaultType XmlShort = new XmlDefaultType(Short.class.getName(), "http://www.w3.org/2001/XMLSchema", "short");
	public static final XmlDefaultType XmlInt = new XmlDefaultType(Integer.class.getName(), "http://www.w3.org/2001/XMLSchema", "int");
	public static final XmlDefaultType XmlLong = new XmlDefaultType(Long.class.getName(), "http://www.w3.org/2001/XMLSchema", "long");
	public static final XmlDefaultType XmlFloat = new XmlDefaultType(Float.class.getName(), "http://www.w3.org/2001/XMLSchema", "decimal");
	public static final XmlDefaultType XmlDouble = new XmlDefaultType(Double.class.getName(), "http://www.w3.org/2001/XMLSchema", "decimal");

	private XmlDefaultType(String clazz, String ns, String name) {
		super(new DummyModel(clazz), ns, name);
	}

	@Override
	public Expression fromString(TypeCache types, Expression v) {
		String cl = javaType().name();
		if (Character.class.getName().equals(cl))
			return new MethodCallExpr(v, "charAt", Utils.list(new IntegerLiteralExpr("0")));
		if (BigDecimal.class.getName().equals(cl) || BigInteger.class.getName().equals(cl))
			return new ObjectCreationExpr(null, types.getClass(cl), new NodeList<>(v));
		return new MethodCallExpr(new TypeExpr(types.get(cl)), "valueOf", Utils.list(v));
	}

	@Override
	public Expression toString(TypeCache types, Expression v) {
		return new MethodCallExpr(v, "toString");
	}
}
