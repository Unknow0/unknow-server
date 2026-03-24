package unknow.server.nio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Histogram;

public class PrometheusListener implements NIOServerListener {
	private static final Logger logger = LoggerFactory.getLogger(PrometheusListener.class);

	private static final Counter ACCEPTED = Counter.build("nio_connection_accepted", "Number of connection accepted").labelNames("name").register();
	private static final Counter CLOSED = Counter.build("nio_connection_closed", "Number of connection closed").labelNames("name").register();
	private static final Histogram SELECT = Histogram.build("nio_select_time", "Duration of the selection process").labelNames("name").buckets(.0001, .0005, .001, .005, .01)
			.register();

	private static final Collection<NIOWorker> WORKERS = new ArrayList<>();

	static {
		new WorkerCollector().register();
	}

	public static final NIOServerListener INSTANCE = new PrometheusListener();

	@Override
	public void starting(NIOServer server) {
		synchronized (WORKERS) {
			WORKERS.addAll(server.workers());
		}
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
	public void closing(NIOServer server, Exception e) {
		synchronized (WORKERS) {
			WORKERS.removeAll(server.workers());
		}
	}

	@Override
	public void onSelect(String name, long now) {
		SELECT.labels(name).observe((System.nanoTime() - now) / 1000_000_000.);
	}

	private static class WorkerCollector extends Collector {

		@Override
		public List<MetricFamilySamples> collect() {
			List<String> labels = Arrays.asList("name");
			GaugeMetricFamily closing = new GaugeMetricFamily("nio_worker_connection_closing", "Number of connection in closing state", labels);
			GaugeMetricFamily writes = new GaugeMetricFamily("nio_worker_connection_writes", "Number of tasks on the worker", labels);
			GaugeMetricFamily tasks = new GaugeMetricFamily("nio_worker_tasks", "Number of tasks on the worker", labels);
			synchronized (WORKERS) {
				for (NIOWorker w : WORKERS) {
					labels = Arrays.asList(w.name);
					closing.addMetric(labels, w.nbClosing());
					tasks.addMetric(labels, w.nbTask());
					try {
						int i = 0;
						for (ConnectionStats s : w.connectionStats().get()) {
							if (s.hasPengingWrite())
								i++;
						}
						writes.addMetric(labels, i);
					} catch (@SuppressWarnings("unused") InterruptedException e) {
						Thread.currentThread().interrupt();
						return Collections.emptyList();
					} catch (ExecutionException e) {
						logger.warn("Failed to get connection stats", e);
					}
				}
			}
			return Arrays.asList(closing, tasks, writes);
		}
	}
}
