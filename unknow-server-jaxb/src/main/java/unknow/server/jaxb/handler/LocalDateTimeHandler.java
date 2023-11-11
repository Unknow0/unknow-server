package unknow.server.jaxb.handler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import unknow.server.jaxb.XmlSimpleHandler;

public class LocalDateTimeHandler implements XmlSimpleHandler<LocalDateTime> {
	public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[XXX]");

	public static final LocalDateTimeHandler INSTANCE = new LocalDateTimeHandler();

	private LocalDateTimeHandler() {
	}

	@Override
	public String toString(LocalDateTime t) {
		return t.format(FMT);
	}

	@Override
	public LocalDateTime toObject(String s) {
		return LocalDateTime.parse(s, FMT);
	}
}
