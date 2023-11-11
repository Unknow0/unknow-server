package unknow.server.jaxb.handler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import unknow.server.jaxb.XmlSimpleHandler;

public class ZonedDateTimeHandler implements XmlSimpleHandler<ZonedDateTime> {
	public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[XXX]");

	public static final ZonedDateTimeHandler INSTANCE = new ZonedDateTimeHandler();

	private ZonedDateTimeHandler() {
	}

	@Override
	public String toString(ZonedDateTime t) {
		return t.format(FMT);
	}

	@Override
	public ZonedDateTime toObject(String s) {
		TemporalAccessor t = FMT.parseBest(s, ZonedDateTime::from, LocalDateTime::from);
		if (t instanceof ZonedDateTime)
			return (ZonedDateTime) t;
		return ((LocalDateTime) t).atZone(ZoneId.systemDefault());
	}
}
