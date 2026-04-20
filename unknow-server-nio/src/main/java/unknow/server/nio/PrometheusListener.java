package unknow.server.nio;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import unknow.server.nio.NIOServerListener.NOPListener;
import unknow.server.nio.NIOWorker.WorkerTask;

public class PrometheusListener extends NOPListener {

	private static final Counter ACCEPTED = Counter.build("nio_connection_accepted", "Number of connection accepted").labelNames("name").register();
	private static final Counter CLOSING = Counter.build("nio_connection_closing", "Number of connection in closing state").labelNames("name").register();
	private static final Counter CLOSED = Counter.build("nio_connection_closed", "Number of connection closed").labelNames("name").register();
	private static final Histogram SELECT = Histogram.build("nio_select_time", "Duration of the selection process").labelNames("name").buckets(.0001, .0005, .001, .005, .01)
			.register();
	private static final Counter TASKS_ACCEPTED = Counter.build("nio_tasks_accepted", "Number of tasks accepted").labelNames("name").register();
	private static final Counter TASKS_DONE = Counter.build("nio_tasks_done", "Number of tasks done").labelNames("name").register();

	public static final NIOServerListener INSTANCE = new PrometheusListener();

	@Override
	public void accepted(String name, NIOConnection h) {
		ACCEPTED.labels(name).inc();
	}

	@Override
	public void accepted(String name, WorkerTask task) {
		TASKS_ACCEPTED.labels(name).inc();
	}

	@Override
	public void done(String name, WorkerTask task) {
		TASKS_DONE.labels(name).inc();
	}

	@Override
	public void closing(String name, NIOConnection h) {
		CLOSING.labels(name).inc();
	}

	@Override
	public void closed(String name, NIOConnection h) {
		CLOSED.labels(name).inc();
	}

	@Override
	public void onSelect(String name, long now) {
		SELECT.labels(name).observe((System.nanoTime() - now) / 1000_000_000.);
	}
}
