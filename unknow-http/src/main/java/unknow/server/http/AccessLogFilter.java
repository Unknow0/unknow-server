package unknow.server.http;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Access log filter <br>
 * the default format is: @{code <br>
 * the allowed format:
 * <dl>
 * <dt>vhost</dt>
 * <dd>container host name</dd>
 * <dt>remote</dt>
 * <dd>param: host,addr,port</dd>
 * <dd>the remote host, address or port</dd>
 * <dt>start</dt>
 * <td>request starting time in iso, rfc for an DateTimeFormater pattern (default to iso)</dd>
 * <dt>end</dt>
 * <dd>request ending time in iso, rfc for an DateTimeFormater pattern (default to iso)</dd>
 * <dt>duration</dt>
 * <dd>the request duration in sec,msec,usec,msec_frac,usec_frac default to sec</dd>
 * <dt>request</td>
 * <dd>line: same as %{request:method} %{request:path}%{request:query} %{request:protocol} (default)
 * <dd>method: requested method</dd>
 * <dd>path: requested path</dd>
 * <dd>query: requested query string</dd>
 * <dd>protocol: requested protocol</dd>
 * <dd>header: requested header value</dd>*
 * <dt>response</td>
 * <dd>status: response status code (default)</dd>
 * <dd>header: requested header value</dd>
 * <dt>user</dt>
 * <dd>the user name or -</dd>
 * </dl>
 */
public class AccessLogFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger("access-log");
	private static final String DEFAULT_FMT = "%{start} %{remote:host} - %{user} \"%{request}\" %{response:status} %{duration}.%{duration:msec_frac}";

	private static final Part[] EMPTY = {};
	private static final Map<String, Function<List<String>, Part>> builders = new HashMap<>();
	static {
		builders.put("vhost", param -> (sb, start, end, req, res) -> sb.append(req.getServletContext().getVirtualServerName()));
		builders.put("remote", param -> {
			String t = param.size() == 0 ? "host" : param.get(0);
			if ("host".equals(t))
				return (sb, start, end, req, res) -> sb.append(req.getRemoteHost());
			if ("addr".equals(t))
				return (sb, start, end, req, res) -> sb.append(req.getRemoteAddr());
			if ("port".equals(t))
				return (sb, start, end, req, res) -> sb.append(req.getRemotePort());
			throw new RuntimeException("unknow remote type '" + t + "'");
		});
		builders.put("start", param -> {
			String type = param.size() > 0 ? param.get(0) : "iso";
			DateTimeFormatter f;
			if (type.equals("iso"))
				f = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
			else if (type.equals("rfc"))
				f = DateTimeFormatter.RFC_1123_DATE_TIME;
			else
				f = DateTimeFormatter.ofPattern(type);
			return (sb, start, end, req, res) -> f.formatTo(start, sb);
		});
		builders.put("end", param -> {
			String type = param.size() > 0 ? param.get(0) : "iso";
			DateTimeFormatter f;
			if (type.equals("iso"))
				f = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
			else if (type.equals("rfc"))
				f = DateTimeFormatter.RFC_1123_DATE_TIME;
			else
				f = DateTimeFormatter.ofPattern(type);
			return (sb, start, end, req, res) -> f.formatTo(end, sb);
		});
		builders.put("duration", param -> {
			String t = param.size() > 0 ? param.get(0) : "sec";

			if (t.equals("sec"))
				return (sb, start, end, req, res) -> sb.append(start.until(end, ChronoUnit.SECONDS));
			if (t.equals("msec"))
				return (sb, start, end, req, res) -> sb.append(start.until(end, ChronoUnit.MILLIS));
			if (t.equals("usec"))
				return (sb, start, end, req, res) -> sb.append(start.until(end, ChronoUnit.MICROS));
			else if (t.equals("msec_frac"))
				return (sb, start, end, req, res) -> sb.append(Duration.between(start, end).getNano() / 1000000);
			else if (t.equals("usec_frac"))
				return (sb, start, end, req, res) -> sb.append(Duration.between(start, end).getNano() / 1000);
			else
				throw new RuntimeException("Unknow duration type '" + t + "'");
		});
		builders.put("request", param -> {
			String t = param.size() == 0 ? "line" : param.get(0);
			if ("line".equals(t))
				return (sb, start, end, req, res) -> {
					sb.append(req.getMethod()).append(' ').append(req.getRequestURI());
					if (req.getQueryString() != null)
						sb.append('?').append(req.getQueryString());
					sb.append(' ').append(req.getProtocol());
				};
			if ("method".equals(t))
				return (sb, start, end, req, res) -> sb.append(req.getMethod());
			if ("uri".equals(t))
				return (sb, start, end, req, res) -> sb.append(req.getRequestURI());
			if ("query".equals(t))
				return (sb, start, end, req, res) -> {
					if (req.getQueryString() != null)
						sb.append(req.getQueryString());
				};
			if ("protocol".equals(t))
				return (sb, start, end, req, res) -> sb.append(req.getProtocol());
			if ("header".equals(t)) {
				if (param.size() < 2)
					throw new RuntimeException("missing param for request header");
				String name = param.get(0);
				return (sb, start, end, req, res) -> sb.append(req.getHeader(name));
			}
			throw new RuntimeException("unknow request type '" + t + "'");
		});
		builders.put("response", param -> {
			String t = param.size() == 0 ? "status" : param.get(0);
			if ("status".equals(t))
				return (sb, start, end, req, res) -> sb.append(res.getStatus());
			if ("header".equals(t)) {
				if (param.size() < 2)
					throw new RuntimeException("missing param for request header");
				String name = param.get(0);
				return (sb, start, end, req, res) -> sb.append(res.getHeader(name));
			}
			throw new RuntimeException("unknow response type '" + t + "'");
		});
		builders.put("user", param -> (sb, start, end, req, res) -> {
			String n = req.getRemoteUser();
			sb.append(n == null ? "-" : n);
		});
	}

	/** format used to log */
	private Part[] parts;

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
		this.parts = parse(format);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String f = filterConfig.getInitParameter("format");
		setFormat(f == null ? DEFAULT_FMT : f);
	}

	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest) || !logger.isInfoEnabled()) {
			chain.doFilter(request, response);
			return;
		}
		LocalDateTime start = LocalDateTime.now();
		try {
			chain.doFilter(request, response);
		} finally {
			LocalDateTime end = LocalDateTime.now();

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < parts.length; i++)
				parts[i].append(sb, start, end, (HttpServletRequest) request, (HttpServletResponse) response);
			logger.info(sb.toString());
		}
	}

	@Override
	public void destroy() { // OK
	}

	@Override
	public String toString() {
		return "AccessLog";
	}

	public static Part[] parse(String template) {
		List<Part> parts = new ArrayList<>();
		List<String> param = new ArrayList<>();
		int last = 0;
		for (;;) {
			int i = template.indexOf("%{", last);
			if (i < 0) {
				parts.add(new StringPart(template.substring(last)));
				break;
			}

			int e = template.indexOf('}', i + 2);
			if (e < 0) {
				parts.add(new StringPart(template.substring(last, i + 2)));
				last = i + 2;
				continue;
			}

			if (last != i)
				parts.add(new StringPart(template.substring(last, i)));
			last = i + 2;

			String key;
			i = template.indexOf(':', last);
			if (i >= 0 && i < e) {
				key = template.substring(last, i);
				last = i + 1;
				while ((i = template.indexOf(':', last)) < e && i > 0) {
					param.add(template.substring(last, i));
					last = i + 1;
				}
				param.add(template.substring(last, e));
			} else
				key = template.substring(last, e);
			Function<List<String>, Part> f = builders.get(key);
			if (f == null)
				throw new RuntimeException("no part named '" + key + "' in template:\n" + template);
			parts.add(f.apply(param));
			param.clear();
			last = e + 1;
		}
		return parts.toArray(EMPTY);
	}

	public static interface Part {
		void append(StringBuilder sb, LocalDateTime start, LocalDateTime end, HttpServletRequest req, HttpServletResponse res);
	}

	public static final class StringPart implements Part {
		private final String str;

		public StringPart(String str) {
			this.str = str;
		}

		@Override
		public void append(StringBuilder sb, LocalDateTime start, LocalDateTime end, HttpServletRequest req, HttpServletResponse res) {
			sb.append(str);
		}
	}
}
