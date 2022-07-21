package unknow.server.maven.jaxws.binding;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.TypeExpr;

import unknow.server.maven.TypeCache;

public class XmlTypePrimitive implements XmlType {
	private final Class<?> clazz;
	private final Class<?> parser;
	private final String parse;
	private final SchemaData schema;

	/**
	 * create new XmlType.XmlPrimitive
	 */
	public XmlTypePrimitive(Class<?> clazz, Class<?> parser, String parse, String type) {
		this.clazz = clazz;
		this.parser = parser;
		this.parse = parse;
		this.schema = new SchemaData(type, "http://www.w3.org/2001/XMLSchema", null, null);
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
	public String clazz() {
		return clazz.getName();
	}

	@Override
	public String binaryName() {
		return clazz.getCanonicalName();
	}

	@Override
	public String toString() {
		return clazz.getName();
	}

	@Override
	public boolean isPrimitive() {
		return clazz.isPrimitive();
	}

	@Override
	public SchemaData schema() {
		return schema;
	}
}