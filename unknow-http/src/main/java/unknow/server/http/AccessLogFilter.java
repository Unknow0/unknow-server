package unknow.server.http;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * simple access log filter
 */
public class AccessLogFilter implements Filter {
	private String format = "%1$tFT%1$tT %2$s %3$s \"%4$s\" %5$s %6$s";

	/**
	 * format used by the access log
	 * <br>1: query timestamp
	 * <br>2: remote address
	 * <br>3: http method
	 * <br>4: request uri
	 * <br>5: http status code
	 * <br>6: request duration in ms
	 * <br>7: client ip (taken from x-forwarded-for)
	 */
	protected void setFormat(String format) {
		this.format = format;
	}

	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest)) {
			chain.doFilter(request, response);
			return;
		}
		HttpServletRequest r = (HttpServletRequest) request;
		String realIp = r.getHeader("x-forwarded-for");
		if (realIp != null)
			realIp = realIp.split(",", 2)[0];
		else
			realIp = r.getRemoteAddr();
		long start = System.currentTimeMillis();
		int status = 500;
		try {
			chain.doFilter(request, response);
			status = ((HttpServletResponse) response).getStatus();
		} finally {
			System.out.format(format, start, r.getRemoteAddr(), r.getMethod(), r.getRequestURI(), status, System.currentTimeMillis() - start);
		}
	}

}
