package unknow.server.jaxb.handler;

import java.time.Duration;

import unknow.server.jaxb.XmlSimpleHandler;

public class DurationHandler implements XmlSimpleHandler<Duration> {
	public static final DurationHandler INSTANCE = new DurationHandler();

	private DurationHandler() {
	}

	@Override
	public String toString(Duration t) {
		return t.toString();
	}

	@Override
	public Duration toObject(String s) {
		return Duration.parse(s);
	}
}
