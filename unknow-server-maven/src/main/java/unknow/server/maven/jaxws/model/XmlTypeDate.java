package unknow.server.maven.jaxws.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.TypeExpr;

import unknow.server.jaxws.DateTimeFormat;
import unknow.server.maven.TypeCache;
import unknow.server.maven.Utils;

public class XmlTypeDate implements XmlType {
	public static final XmlType DURATION = new XmlTypeDate(Duration.class.getCanonicalName(), Type.DURATION);
	public static final XmlType LOCAL_DATE = new XmlTypeDate(LocalDate.class.getCanonicalName(), Type.DATE);
	public static final XmlType LOCAL_TIME = new XmlTypeDate(LocalTime.class.getCanonicalName(), Type.TIME);
	public static final XmlType LOCAL_DATETIME = new XmlTypeDate(LocalDateTime.class.getCanonicalName(), Type.DATETIME);
	public static final XmlType OFFSET_TIME = new XmlTypeDate(OffsetTime.class.getCanonicalName(), Type.TIME);
	public static final XmlType OFFSET_DATETIME = new XmlTypeDate(OffsetDateTime.class.getCanonicalName(), Type.DATETIME);
	public static final XmlType ZONED_DATETIME = new XmlTypeDate(ZonedDateTime.class.getCanonicalName(), Type.DATETIME);

	private static enum Type {
		DURATION("duration"), DATE("date"), TIME("time"), DATETIME("dateTime");

		private final String name;

		private Type(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private final String clazz;
	private final Type type;
	private final SchemaData schema;

	private XmlTypeDate(String clazz, Type type) {
		this.clazz = clazz;
		this.type = type;
		this.schema = new SchemaData(type.toString(), "http://www.w3.org/2001/XMLSchema", null, null);
	}

	@Override
	public Expression convert(TypeCache types, Expression v) {
		return new MethodCallExpr(new TypeExpr(types.get(clazz)), "parse", Utils.list(v, new FieldAccessExpr(new TypeExpr(types.get(DateTimeFormat.class)), type.name())));
	}

	@Override
	public Expression toString(TypeCache types, Expression v) {
		return new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(DateTimeFormat.class)), type.name()), "format", Utils.list(v));
	}

	@Override
	public String clazz() {
		return clazz;
	}

	@Override
	public String binaryName() {
		return clazz + ';';
	}

	@Override
	public SchemaData schema() {
		return schema;
	}

}
