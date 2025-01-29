package unknow.server.nio.listener;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import unknow.server.nio.NIOConnectionAbstract;
import unknow.server.nio.NIOServer;
import unknow.server.nio.NIOServerListener;

public class PrometheusListener implements NIOServerListener {

	private final Counter counter;
	private final Gauge current;

	public PrometheusListener() {
		this(CollectorRegistry.defaultRegistry);
	}

	public PrometheusListener(CollectorRegistry registry) {
		counter = Counter.build("nio_server_connection_total", "number of connection accepted since the start").labelNames("worker").register(registry);
		current = Gauge.build("nio_server_connection_current", "number of connection curently active").labelNames("worker").register(registry);
	}

	@Override
	public void starting(NIOServer server) {
		// nothing
	}

	@Override
	public void accepted(int id, NIOConnectionAbstract h) {
		counter.labels(Integer.toString(id)).inc();
		current.labels(Integer.toString(id)).inc();
	}

	@Override
	public void closed(int id, NIOConnectionAbstract h) {
		current.labels(Integer.toString(id)).dec();
	}

	@Override
	public void closing(NIOServer server, Exception e) {
		// nothing
	}

}
