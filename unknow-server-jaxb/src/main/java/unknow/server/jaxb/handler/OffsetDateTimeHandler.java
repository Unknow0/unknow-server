package unknow.server.jaxb.handler;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import unknow.server.jaxb.XmlSimpleHandler;

public class OffsetDateTimeHandler implements XmlSimpleHandler<OffsetDateTime> {
	public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[XXX]");

	public static final OffsetDateTimeHandler INSTANCE = new OffsetDateTimeHandler();

	private OffsetDateTimeHandler() {
	}

	@Override
	public String toString(OffsetDateTime t) {
		return t.format(FMT);
	}

	@Override
	public OffsetDateTime toObject(String s) {
		TemporalAccessor t = FMT.parseBest(s, OffsetDateTime::from, LocalDateTime::from);
		if (t instanceof OffsetDateTime)
			return (OffsetDateTime) t;
		return ((LocalDateTime) t).atOffset(OffsetDateTime.now().getOffset());
	}
}
