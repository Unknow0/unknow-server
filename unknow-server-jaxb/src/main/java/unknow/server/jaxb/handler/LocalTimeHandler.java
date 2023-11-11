package unknow.server.jaxb.handler;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import unknow.server.jaxb.XmlSimpleHandler;

public class LocalTimeHandler implements XmlSimpleHandler<LocalTime> {
	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss[.S][XXX]");

	public static final LocalTimeHandler INSTANCE = new LocalTimeHandler();

	private LocalTimeHandler() {
	}

	@Override
	public String toString(LocalTime t) {
		return t.format(FMT);
	}

	@Override
	public LocalTime toObject(String s) {
		return LocalTime.parse(s, FMT);
	}
}
