package unknow.server.servlet;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * <dd>request starting time in iso, rfc for an DateTimeFormater pattern (default to iso)</dd>
 * <dt>end</dt>
 * <dd>request ending time in iso, rfc for an DateTimeFormater pattern (default to iso)</dd>
 * <dt>duration</dt>
 * <dd>the request duration in sec,msec,usec,msec_frac,usec_frac default to sec</dd>
 * <dt>request</dt>
 * <dd>line: same as %{request:method} %{request:path}%{request:query} %{request:protocol} (default)</dd>
 * <dd>method: requested method</dd>
 * <dd>path: requested path</dd>
 * <dd>query: requested query string</dd>
 * <dd>protocol: requested protocol</dd>
 * <dd>header: requested header value</dd>
 * <dt>response</dt>
 * <dd>status: response status code (default)</dd>
 * <dd>header: requested header value</dd>
 * <dt>user</dt>
 * <dd>the user name or -</dd>
 * </dl>
 */
public class AccessLogFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger("access-log");
	private static final String DEFAULT_FMT = "%{start} %{remote:host} - %{user} \"%{request}\" %{response:status} %{duration}.%{duration:msec_frac}";

	private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

	private static final Part[] EMPTY = {};
	private static final Map<String, PartBuilder> builders = new HashMap<>();
	static {
		builders.put("vhost", param -> (sb, start, end, req, res) -> sb.append(req.getServletContext().getVirtualServerName()));
		builders.put("remote", param -> {
			String t = param.isEmpty() ? "host" : param.get(0);
			if ("host".equals(t))
				return (sb, start, end, req, res) -> sb.append(req.getRemoteHost());
			if ("addr".equals(t))
				return (sb, start, end, req, res) -> sb.append(req.getRemoteAddr());
			if ("port".equals(t))
				return (sb, start, end, req, res) -> sb.append(req.getRemotePort());
			throw new ServletException("unknow remote type '" + t + "'");
		});
		builders.put("start", param -> {
			DateTimeFormatter f = getFormater(param.isEmpty() ? "iso" : param.get(0));
			return (sb, start, end, req, res) -> f.formatTo(start, sb);
		});
		builders.put("end", param -> {
			DateTimeFormatter f = getFormater(param.isEmpty() ? "iso" : param.get(0));
			return (sb, start, end, req, res) -> f.formatTo(end, sb);
		});
		builders.put("duration", param -> {
			String t = param.isEmpty() ? "sec" : param.get(0);

			if (t.equals("sec"))
				return (sb, start, end, req, res) -> sb.append(start.until(end, ChronoUnit.SECONDS));
			if (t.equals("msec"))
				return (sb, start, end, req, res) -> sb.append(start.until(end, ChronoUnit.MILLIS));
			if (t.equals("usec"))
				return (sb, start, end, req, res) -> sb.append(start.until(end, ChronoUnit.MICROS));
			else if (t.equals("msec_frac"))
				return (sb, start, end, req, res) -> sb.append(String.format("%03d", Duration.between(start, end).getNano() / 1000000));
			else if (t.equals("usec_frac"))
				return (sb, start, end, req, res) -> sb.append(String.format("%06d", Duration.between(start, end).getNano() / 1000));
			else
				throw new ServletException("Unknow duration type '" + t + "'");
		});
		builders.put("request", param -> {
			String t = param.isEmpty() ? "line" : param.get(0);
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
					throw new ServletException("missing param for request header");
				String name = param.get(1);
				return (sb, start, end, req, res) -> sb.append(req.getHeader(name));
			}
			throw new ServletException("unknow request type '" + t + "'");
		});
		builders.put("response", param -> {
			String t = param.isEmpty() ? "status" : param.get(0);
			if ("status".equals(t))
				return (sb, start, end, req, res) -> sb.append(res.getStatus());
			if ("header".equals(t)) {
				if (param.size() < 2)
					throw new ServletException("missing param for request header");
				String name = param.get(1);
				return (sb, start, end, req, res) -> sb.append(res.getHeader(name));
			}
			throw new ServletException("unknow response type '" + t + "'");
		});
		builders.put("user", param -> (sb, start, end, req, res) -> {
			String n = req.getRemoteUser();
			sb.append(n == null ? "-" : n);
		});
	}

	private static final ThreadLocal<StringBuilder> SB = ThreadLocal.withInitial(() -> new StringBuilder());

	/** format used to log */
	private Part[] parts;

	/**
	 * format used by the access log <br>
	 * 
	 * @param format the format
	 * @throws ServletException on error
	 */
	protected void setFormat(String format) throws ServletException {
		this.parts = parse(format);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String f = filterConfig.getInitParameter("format");
		if (f == null)
			f = System.getProperty("ACCESS_LOG_FMT");
		if (f == null)
			f = System.getenv("ACCESS_LOG_FMT");
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

			StringBuilder sb = SB.get();
			for (int i = 0; i < parts.length; i++)
				parts[i].append(sb, start, end, (HttpServletRequest) request, (HttpServletResponse) response);
			logger.info("{}", sb);
			sb.setLength(0);
		}
	}

	@Override
	public void destroy() { // OK
	}

	@Override
	public String toString() {
		return "AccessLog";
	}

	/**
	 * parse the template
	 * 
	 * @param template the template
	 * @return the part
	 * @throws ServletException on error
	 */
	public static Part[] parse(String template) throws ServletException {
		List<Part> parts = new ArrayList<>();
		List<String> param = new ArrayList<>();
		int last = 0;
		int i;
		while ((i = template.indexOf("%{", last)) >= 0) {
			int e = template.indexOf('}', i + 2);
			if (e < 0) {
				parts.add(new StringPart(template.substring(last, i + 2)));
				last = i + 2;
				continue;
			}

			if (last != i)
				parts.add(new StringPart(template.substring(last, i)));

			String key = parseFormat(template, i + 2, e, param);
			PartBuilder f = builders.get(key);
			if (f == null)
				throw new ServletException("no part named '" + key + "' in template:\n" + template);
			parts.add(f.apply(param));
			param.clear();
			last = e + 1;
		}
		if (last < template.length())
			parts.add(new StringPart(template.substring(last)));
		return parts.toArray(EMPTY);
	}

	/**
	 * parse a format
	 * 
	 * @param template the template
	 * @param o start index for the format
	 * @param e end index for the format
	 * @param param output param
	 * @return the format name
	 */
	private static final String parseFormat(String template, int o, int e, List<String> param) {
		int i = template.indexOf(':', o);
		if (i < 0 || i > e)
			return template.substring(o, e);
		String key = template.substring(o, i);
		o = i + 1;
		while ((i = template.indexOf(':', o)) < e && i > 0) {
			param.add(template.substring(o, i));
			o = i + 1;
		}
		param.add(template.substring(o, e));
		return key;
	}

	private static DateTimeFormatter getFormater(String type) {
		if (type.equals("iso"))
			return ISO;
		if (type.equals("rfc"))
			return DateTimeFormatter.RFC_1123_DATE_TIME;
		return DateTimeFormatter.ofPattern(type);
	}

	/** a part */
	public static interface Part {
		/**
		 * append this part
		 * 
		 * @param sb where to
		 * @param start start time
		 * @param end end time
		 * @param req request
		 * @param res response
		 * @throws ServletException on error
		 */
		void append(StringBuilder sb, LocalDateTime start, LocalDateTime end, HttpServletRequest req, HttpServletResponse res) throws ServletException;
	}

	/** static text */
	public static final class StringPart implements Part {
		private final String str;

		/** @param str the text */
		public StringPart(String str) {
			this.str = str;
		}

		@Override
		public void append(StringBuilder sb, LocalDateTime start, LocalDateTime end, HttpServletRequest req, HttpServletResponse res) {
			sb.append(str);
		}
	}

	/** part builder */
	public static interface PartBuilder {
		/**
		 * create a new part
		 * 
		 * @param param params
		 * @return the part
		 * @throws ServletException on error
		 */
		Part apply(List<String> param) throws ServletException;
	}
}
