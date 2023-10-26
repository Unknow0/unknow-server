package unknow.server.jaxb.handler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import unknow.server.jaxb.XmlSimpleHandler;

public class LocalDateHandler implements XmlSimpleHandler<LocalDate> {
	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd[XXX]");

	public static final LocalDateHandler INSTANCE = new LocalDateHandler();

	private LocalDateHandler() {
	}

	@Override
	public String toString(LocalDate t) {
		return t.format(FMT);
	}

	@Override
	public LocalDate toObject(String s) {
		return LocalDate.parse(s, FMT);
	}
}
