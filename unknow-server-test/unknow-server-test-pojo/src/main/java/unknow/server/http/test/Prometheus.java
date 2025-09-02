package unknow.server.http.test;

import io.prometheus.client.servlet.jakarta.exporter.MetricsServlet;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/-/metrics")
public class Prometheus extends MetricsServlet {
	private static final long serialVersionUID = 1L;
}