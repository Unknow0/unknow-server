/**
 * 
 */
package unknow.server.maven.jaxws.model;

import com.github.javaparser.ast.expr.Expression;

import unknow.server.maven.TypeCache;

/**
 * @author unknow
 */
public class XmlEnum implements XmlType {
	public final String clazz;
	public final XmlEnumEntry[] entries;
	private final SchemaData schema;

	public XmlEnum(String clazz, XmlEnumEntry[] entries, SchemaData schema) {
		this.clazz = clazz;
		this.entries = entries;
		this.schema = schema;
	}

	@Override
	public Expression convert(TypeCache types, Expression v) {
		return v;
	}

	@Override
	public String binaryName() {
		return clazz + ";";
	}

	@Override
	public SchemaData schema() {
		return schema;
	}

	public class XmlEnumEntry {
		public final String name;
		public final String value;

		public XmlEnumEntry(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
}