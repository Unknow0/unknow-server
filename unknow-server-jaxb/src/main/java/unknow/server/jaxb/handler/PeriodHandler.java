package unknow.server.jaxb.handler;

import java.time.Period;

import unknow.server.jaxb.XmlSimpleHandler;

public class PeriodHandler implements XmlSimpleHandler<Period> {
	public static final PeriodHandler INSTANCE = new PeriodHandler();

	private PeriodHandler() {
	}

	@Override
	public String toString(Period t) {
		return t.toString();
	}

	@Override
	public Period toObject(String s) {
		return Period.parse(s);
	}
}
