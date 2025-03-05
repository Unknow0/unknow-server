package unknow.server.servlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.Histogram;

public class PrometheusHandler extends ChannelDuplexHandler {
	private static final Gauge activeConnections = Gauge.build().name("netty_connections_total").help("Current number of active connections").labelNames("addr").register();
	private static final Counter totalConnections = Counter.build().name("netty_connections_total").help("Total number of opened connections").labelNames("addr").register();
	private static final Counter errors = Counter.build().name("netty_errors_total").help("Total number of errors").labelNames("addr", "exception").register();

	private static final Histogram requestLatency = Histogram.build().name("netty_request_latency_seconds").help("Request latency in seconds").register();

	public static final PrometheusHandler HANDLER = new PrometheusHandler();

	private final PooledByteBufCollector collector = new PooledByteBufCollector().register();

	private PrometheusHandler() {
		collector.track(PooledByteBufAllocator.DEFAULT);
		collector.track(UnpooledByteBufAllocator.DEFAULT);
	}

	@Override
	public boolean isSharable() {
		return true;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (ctx.alloc() instanceof ByteBufAllocatorMetricProvider)
			collector.track((ByteBufAllocatorMetricProvider) ctx.alloc());
		String addr = ctx.channel().localAddress().toString();
		totalConnections.labels(addr).inc();
		activeConnections.labels(addr).inc();
		ctx.fireChannelActive();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		String addr = ctx.channel().localAddress().toString();
		activeConnections.labels(addr).dec();
		ctx.fireChannelInactive();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		String addr = ctx.channel().localAddress().toString();
		errors.labels(addr, cause.getClass().getSimpleName()).inc();
		ctx.fireExceptionCaught(cause);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		String addr = ctx.channel().localAddress().toString();
		long startTime = System.nanoTime();
		ctx.fireChannelRead(msg);
		long duration = System.nanoTime() - startTime;
		requestLatency.labels(addr).observe(TimeUnit.NANOSECONDS.toSeconds(duration));
	}

	private static final class PooledByteBufCollector extends Collector {
		private static final List<String> LABEL = Arrays.asList("name");

		private final Map<ByteBufAllocatorMetricProvider, Object> map = new ConcurrentHashMap<>();

		public void track(ByteBufAllocatorMetricProvider alloc) {
			map.put(alloc, this);
		}

		private static final String name(ByteBufAllocatorMetricProvider alloc) {
			if (alloc == PooledByteBufAllocator.DEFAULT)
				return "pooled";
			if (alloc == UnpooledByteBufAllocator.DEFAULT)
				return "unpooled";
			return alloc.toString();
		}

		@Override
		public List<MetricFamilySamples> collect() {
			List<MetricFamilySamples> metrics = new ArrayList<>();

			GaugeMetricFamily direct = new GaugeMetricFamily("netty_pooled_bytebuf_arena_chunk", "ByteBuf allocator chunksize", LABEL);
			GaugeMetricFamily heap = new GaugeMetricFamily("netty_pooled_bytebuf_arena_chunk", "ByteBuf allocator chunksize", LABEL);
			for (ByteBufAllocatorMetricProvider alloc : map.keySet()) {
				List<String> label = Arrays.asList(name(alloc));

				ByteBufAllocatorMetric m = alloc.metric();
				heap.addMetric(label, m.usedHeapMemory());
				direct.addMetric(label, m.usedHeapMemory());
			}

			return metrics;
		}
	}
}
