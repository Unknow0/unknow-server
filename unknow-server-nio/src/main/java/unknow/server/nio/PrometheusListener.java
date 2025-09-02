package unknow.server.nio;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

public class PrometheusListener implements NIOServerListener {
	private static final Counter ACCEPTED = Counter.build("nio_connection_accepted", "Number of connection accepted").labelNames("name").register();
	private static final Counter CLOSED = Counter.build("nio_connection_closed", "Number of connection closed").labelNames("name").register();
	private static final Histogram SELECT = Histogram.build("nio_select_time", "Duration of the selection process").labelNames("name").register();

	@Override
	public void starting(NIOServer server) { // ok
	}

	@Override
	public void accepted(String name, NIOConnection h) {
		ACCEPTED.labels(name).inc();
	}

	@Override
	public void closed(String name, NIOConnection h) {
		CLOSED.labels(name).inc();
	}

	@Override
	public void closing(NIOServer server, Exception e) { // ok
	}

	@Override
	public void onSelect(String name, long now) {
		SELECT.labels(name).observe((System.nanoTime() - now) / 1000_000_000.);
	}
}
