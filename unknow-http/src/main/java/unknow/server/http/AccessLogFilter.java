package unknow.server.http;

import java.io.IOException;
import java.util.Formatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * simple access log filter TODO: replace Formatter
 */
public class AccessLogFilter implements Filter {
	/** global fomatter to use */
	private static final Formatter f = new Formatter(System.out);
	/** pending entry to write */
	private final BlockingQueue<Entry> queue = new LinkedBlockingQueue<>();

	/** format used to log */
	private String format = "%1$tFT%1$tT %2$s %3$s \"%4$s\" %5$d %6$d%n";

	private final Thread t = new Thread() {
		@Override
		public void run() {
			try {
				while (!isInterrupted()) {
					log(f, queue.take());
				}
			} catch (InterruptedException e) { // OK
			}
		}
	};

	/**
	 * format used by the access log <br>
	 * 1: query timestamp <br>
	 * 2: remote address <br>
	 * 3: http method <br>
	 * 4: request uri <br>
	 * 5: http status code <br>
	 * 6: request duration in ms <br>
	 * 7: client ip (taken from x-forwarded-for, or remote address if not found)
	 */
	protected void setFormat(String format) {
		this.format = format;
		// validate format
		log(new Formatter(), new Entry());
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String f = filterConfig.getInitParameter("format");
		if (f != null)
			setFormat(f);
		t.start();
	}

	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest)) {
			chain.doFilter(request, response);
			return;
		}
		long start = System.currentTimeMillis();
		try {
			chain.doFilter(request, response);
		} finally {
			queue.offer(new Entry(start, (HttpServletRequest) request, (HttpServletResponse) response));
		}
	}

	@Override
	public void destroy() {
		t.interrupt();
	}

	protected void log(Formatter f, Entry e) {
		f.format(format, e.arg);
	}

	private static final class Entry {
		private final Object[] arg;

		public Entry() {
			arg = new Object[] { System.currentTimeMillis(), "remote", "TEST", "/", 200, 42, "realIp" };
		}

		public Entry(long start, HttpServletRequest req, HttpServletResponse res) {
			arg = new Object[] { start, req.getRemoteAddr(), req.getMethod(), req.getRequestURI(), res.getStatus(), System.currentTimeMillis() - start, getRealIp(req) };
		}

		private static String getRealIp(HttpServletRequest req) {
			String realIp = req.getHeader("x-forwarded-for");
			if (realIp == null)
				return req.getRemoteAddr();
			return realIp.split(",", 2)[0];
		}
	}
}
