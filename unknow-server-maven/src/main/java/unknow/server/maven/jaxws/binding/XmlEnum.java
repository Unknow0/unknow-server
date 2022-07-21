/**
 * 
 */
package unknow.server.maven.jaxws.binding;

import java.util.List;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;

/**
 * @author unknow
 */
public class XmlEnum implements XmlType {
	public final String clazz;
	public final List<XmlEnumEntry> entries;
	public final String convertMethod;
	private final SchemaData schema;

	public XmlEnum(String clazz, List<XmlEnumEntry> entries, SchemaData schema, String convertMethod) {
		this.clazz = clazz;
		this.entries = entries;
		this.convertMethod = convertMethod;
		this.schema = schema;
	}

	@Override
	public Expression convert(TypeCache types, Expression v) {
		return new MethodCallExpr(null, convertMethod, Utils.list(v));
	}

	@Override
	public Expression toString(TypeCache types, Expression v) {
		return new MethodCallExpr(null, convertMethod, Utils.list(v));
	}

	@Override
	public String clazz() {
		return clazz;
	}

	@Override
	public SchemaData schema() {
		return schema;
	}

	public static final class XmlEnumEntry {
		public final String name;
		public final String value;

		public XmlEnumEntry(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
}