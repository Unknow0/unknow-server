package unknow.server.servlet;

import java.io.IOException;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.Histogram.Timer;
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
	private static final int pathComponents = -1;

	private Histogram times;
	private Counter status;
	private Counter inProgress;

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
		inProgress = Counter.build(METRIC + "_started", "HTTP request in progress").labelNames("path", "method", "ip").register();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest)) {
			chain.doFilter(request, response);
			return;
		}
		HttpServletRequest req = (HttpServletRequest) request;
		String method = req.getMethod();
		String components = getComponents(req.getRequestURI());
		String ip = req.getHeader("X-Forwarded-For");
		if (ip != null)
			ip = ip.split(",", 2)[0];
		else
			ip = req.getRemoteAddr();

		inProgress.labels(components, method, ip).inc();
		try (Timer timer = times.labels(components, method, ip).startTimer()) {
			chain.doFilter(request, response);
		} finally {
			String code = Integer.toString(((HttpServletResponse) response).getStatus());
			status.labels(components, method, ip, code).inc();
		}
	}

	@SuppressWarnings("unused")
	private static String getComponents(String str) {
		if (str == null || pathComponents < 1)
			return str;
		int count = 0;
		int i = -1;
		do {
			i = str.indexOf("/", i + 1);
			if (i < 0)
				return str;
			count++;
		} while (count <= pathComponents);

		return str.substring(0, i);
	}
}