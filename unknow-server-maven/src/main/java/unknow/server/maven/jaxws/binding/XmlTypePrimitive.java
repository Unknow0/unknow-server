package unknow.server.maven.jaxws.binding;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.TypeExpr;

import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;
import unknow.server.maven.model.PrimitiveModel;

public class XmlTypePrimitive extends XmlType<PrimitiveModel> {
	public static final XmlTypePrimitive XmlBoolean = new XmlTypePrimitive(PrimitiveModel.BOOLEAN);
	public static final XmlTypePrimitive XmlByte = new XmlTypePrimitive(PrimitiveModel.BYTE);
	public static final XmlTypePrimitive XmlChar = new XmlTypePrimitive(PrimitiveModel.CHAR);
	public static final XmlTypePrimitive XmlShort = new XmlTypePrimitive(PrimitiveModel.SHORT);
	public static final XmlTypePrimitive XmlInt = new XmlTypePrimitive(PrimitiveModel.INT);
	public static final XmlTypePrimitive XmlLong = new XmlTypePrimitive(PrimitiveModel.LONG);
	public static final XmlTypePrimitive XmlFloat = new XmlTypePrimitive(PrimitiveModel.FLOAT);
	public static final XmlTypePrimitive XmlDouble = new XmlTypePrimitive(PrimitiveModel.DOUBLE);

	/**
	 * create new XmlType.XmlPrimitive
	 */
	private XmlTypePrimitive(PrimitiveModel clazz) {
		super(clazz, "http://www.w3.org/2001/XMLSchema", clazz == PrimitiveModel.CHAR ? "string" : clazz.name());
	}

	@Override
	public Expression fromString(TypeCache types, Expression v) {
		if (PrimitiveModel.BYTE == javaType())
			return new MethodCallExpr(new TypeExpr(types.get(Byte.class)), "parseByte", new NodeList<>(v));
		if (PrimitiveModel.SHORT == javaType())
			return new MethodCallExpr(new TypeExpr(types.get(Short.class)), "parseShort", new NodeList<>(v));
		if (PrimitiveModel.CHAR == javaType())
			return new MethodCallExpr(v, "charAt", Utils.list(new IntegerLiteralExpr("0")));
		if (PrimitiveModel.INT == javaType())
			return new MethodCallExpr(new TypeExpr(types.get(Integer.class)), "parseInt", new NodeList<>(v));
		if (PrimitiveModel.LONG == javaType())
			return new MethodCallExpr(new TypeExpr(types.get(Long.class)), "parseLong", new NodeList<>(v));
		if (PrimitiveModel.FLOAT == javaType())
			return new MethodCallExpr(new TypeExpr(types.get(Float.class)), "parseFLoat", new NodeList<>(v));
		if (PrimitiveModel.DOUBLE == javaType())
			return new MethodCallExpr(new TypeExpr(types.get(Double.class)), "parseDouble", new NodeList<>(v));
		throw new RuntimeException("unknown primitive type " + javaType());
	}

	@Override
	public Expression toString(TypeCache types, Expression v) {
		return new MethodCallExpr(new TypeExpr(types.get(String.class)), "valueOf", new NodeList<>(v));
	}
}