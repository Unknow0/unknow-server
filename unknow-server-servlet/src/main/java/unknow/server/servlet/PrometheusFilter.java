package unknow.server.servlet;

import java.io.IOException;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class PrometheusFilter implements Filter {
	private static final String HELP = "The time taken fulfilling servlet requests";
	private static final String METRIC = "http_requests";
	private static final double[] DEFAULT_BUCKET = { .001, .01, .1, 1, 2, 5, 10, 60 };

	private Histogram times;
	private Counter status;
	private Counter started;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String init = filterConfig.getInitParameter("bucket");
		double[] buckets = DEFAULT_BUCKET;
		if (init != null) {
			String[] split = init.split(",");
			buckets = new double[split.length];
			for (int i = 0; i < split.length; i++)
				buckets[i] = Double.parseDouble(split[i]);
		}
		times = Histogram.build(METRIC, HELP).labelNames("path", "method", "ip").buckets(buckets).register();

		status = Counter.build(METRIC + "_status_total", "HTTP status codes").labelNames("path", "method", "ip", "status").register();
		started = Counter.build(METRIC + "_started", "HTTP request in progress").labelNames("method", "ip").register();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest)) {
			chain.doFilter(request, response);
			return;
		}
		HttpServletRequest req = (HttpServletRequest) request;
		String method = req.getMethod();
		String ip = req.getHeader("X-Forwarded-For");
		if (ip != null)
			ip = ip.split(",", 2)[0];
		else
			ip = req.getRemoteAddr();

		started.labels(method, ip).inc();
		long start = System.nanoTime();
		try {
			chain.doFilter(request, response);
		} finally {
			String code = Integer.toString(((HttpServletResponse) response).getStatus());
			String path = (String) req.getAttribute("requestPattern");
			if (path == null)
				path = req.getRequestURI();
			status.labels(path, method, ip, code).inc();
			times.labels(path, method, ip).observe((System.nanoTime() - start) / 1.e9);
		}
	}
}