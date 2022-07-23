package unknow.server.maven.jaxws.binding;

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

public class XmlTypeDate extends XmlType<DummyModel> {
	public static final XmlTypeDate DURATION = new XmlTypeDate(Duration.class.getCanonicalName(), Type.DURATION);
	public static final XmlTypeDate LOCAL_DATE = new XmlTypeDate(LocalDate.class.getCanonicalName(), Type.DATE);
	public static final XmlTypeDate LOCAL_TIME = new XmlTypeDate(LocalTime.class.getCanonicalName(), Type.TIME);
	public static final XmlTypeDate LOCAL_DATETIME = new XmlTypeDate(LocalDateTime.class.getCanonicalName(), Type.DATETIME);
	public static final XmlTypeDate OFFSET_TIME = new XmlTypeDate(OffsetTime.class.getCanonicalName(), Type.TIME);
	public static final XmlTypeDate OFFSET_DATETIME = new XmlTypeDate(OffsetDateTime.class.getCanonicalName(), Type.DATETIME);
	public static final XmlTypeDate ZONED_DATETIME = new XmlTypeDate(ZonedDateTime.class.getCanonicalName(), Type.DATETIME);

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

	private XmlTypeDate(String clazz, Type type) { // TODO
		super(new DummyModel(clazz), "http://www.w3.org/2001/XMLSchema", type.name);
	}

	@Override
	public Expression fromString(TypeCache types, Expression v) {
		return new MethodCallExpr(new TypeExpr(types.get(javaType().name())), "parse", Utils.list(v, new FieldAccessExpr(new TypeExpr(types.get(DateTimeFormat.class)), javaType().name())));
	}

	@Override
	public Expression toString(TypeCache types, Expression v) {
		return new MethodCallExpr(new FieldAccessExpr(new TypeExpr(types.get(DateTimeFormat.class)), javaType().name()), "format", Utils.list(v));
	}
}
