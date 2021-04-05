/**
 * 
 */
package unknow.server.http.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import unknow.server.http.HttpHandler;
import unknow.server.http.utils.ArrayMap;

/**
 * @author unknow
 */
public class ServletRequestImpl implements HttpServletRequest {
	private final ArrayMap<Object> attributes = new ArrayMap<>();

	private final ServletContextImpl ctx;
	public final HttpHandler req;
	private final DispatcherType type;

	private String protocol = null;
	private String method = null;
	private String servletPath = null;
	private String pathInfo = null;
	private String query = null;

	private String encoding = null;
	private long contentLength = -2;

	private Map<String, List<String>> headers;
	private Map<String, String[]> parameter;

	private String remoteAddr;
	private String remoteHost;
	private String localAddr;
	private String localHost;

	/**
	 * create new ServletRequestImpl
	 * 
	 * @param ctx  the context
	 * @param req  the raw request
	 * @param type dispatcher type of this request
	 */
	public ServletRequestImpl(ServletContextImpl ctx, HttpHandler req, DispatcherType type) {
		this.ctx = ctx;
		this.req = req;
		this.type = type;
	}

	private void parseParam() {
		if (parameter != null)
			return;
		parameter = new HashMap<>();
//		Map<String, List<String>> p = new HashMap<>();
//		if (!req.query.isEmpty()) {
//			int i, j = 0;
//			do {
//				i = req.query.indexOf((byte) '&', j, -1);
//				int k = req.query.indexOf((byte) '=', j, i);
//				sb.setLength(0);
//				req.query.toString(sb, j, k);
//				String key = sb.toString();
//				List<String> list = p.get(key);
//				if (list == null)
//					p.put(key, list = new ArrayList<>());
//				if (k > 0) {
//					sb.setLength(0);
//					req.query.toString(sb, k, i);
//					list.add(sb.toString());
//				}
//			} while (i > 0);
//		}
//		// TODO content-type application/x-www-form-urlencoded
//
//		String[] s = new String[0];
//		for (Entry<String, List<String>> e : p.entrySet())
//			parameter.put(e.getKey(), e.getValue().toArray(s));
	}

	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return attributes.names();
	}

	@Override
	public void removeAttribute(String name) {
		setAttribute(name, null);
	}

	@Override
	public void setAttribute(String name, Object o) {
		Object old = attributes.set(name, o);
		ctx.getEvents().fireRequestAttribute(this, name, o, old);
	}

	@Override
	public String getCharacterEncoding() {
		if (encoding == null) {
			// TODO get from request
			if (encoding == null)
				encoding = ctx.getRequestCharacterEncoding();
		}
		return encoding;
	}

	@Override
	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
		encoding = env;
	}

	@Override
	public String getHeader(String name) {
		if (headers == null)
			headers = req.parseHeader();
		List<String> list = headers.get(name.toLowerCase());
		return list == null || list.isEmpty() ? null : list.get(0);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		if (headers == null)
			headers = req.parseHeader();
		List<String> list = headers.get(name.toLowerCase());
		return list == null || list.isEmpty() ? Collections.emptyEnumeration() : Collections.enumeration(list);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		if (headers == null)
			headers = req.parseHeader();
		return Collections.enumeration(headers.keySet());
	}

	@Override
	public long getDateHeader(String name) {
		String header = getHeader(name);
		if (header == null)
			return -1;
		try {
			Instant from = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(name));
			return from.getEpochSecond() * 1000 + from.getNano() / 1000000;
		} catch (DateTimeException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public int getIntHeader(String name) {
		String header = getHeader(name);
		return header == null ? -1 : Integer.parseInt(header);
	}

	@Override
	public String getContentType() {
		return getHeader("content-type");
	}

	@Override
	public Locale getLocale() {
		// Accept-Language
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<Locale> getLocales() {
		// TODO Auto-generated method stub
		// Accept-Language
		// or default Local
		return null;
	}

	@Override
	public int getContentLength() {
		long l = getContentLengthLong();
		return l > Integer.MAX_VALUE ? -1 : (int) l;
	}

	@Override
	public long getContentLengthLong() {
		if (contentLength == -2) {
			String l = getHeader("content-length");
			contentLength = l == null || l.isEmpty() ? -1 : Long.parseLong(l);
		}
		return contentLength;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		if (parameter == null)
			parseParam();
		return Collections.enumeration(parameter.keySet());
	}

	@Override
	public String[] getParameterValues(String name) {
		if (parameter == null)
			parseParam();
		return parameter.get(name);
	}

	@Override
	public String getParameter(String name) {
		if (parameter == null)
			parseParam();
		String[] strings = parameter.get(name);
		return strings == null || strings.length == 0 ? null : strings[0];
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		if (parameter == null)
			parseParam();
		return Collections.unmodifiableMap(parameter);
	}

	@Override
	public String getScheme() {
		return "http";
	}

	@Override
	public String getMethod() {
		if (method == null)
			method = req.parseMethod();
		return method;
	}

	@Override
	public String getQueryString() {
		if (query == null)
			query = req.parseQuery();
		return query.isEmpty() ? null : query;
	}

	@Override
	public String getProtocol() {
		if (protocol == null)
			protocol = req.parseProtocol();
		return protocol;
	}

	@Override
	public String getServerName() {
		return ctx.getVirtualServerName();
	}

	@Override
	public int getServerPort() {
		return getLocalPort();
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Deprecated
	@Override
	public String getRealPath(String path) {
		return null;
	}

	@Override
	public String getRemoteAddr() {
		if (remoteAddr == null)
			remoteAddr = getAddr(req.getRemote());
		return remoteAddr;
	}

	@Override
	public String getRemoteHost() {
		if (remoteHost == null)
			remoteHost = req.getRemote().getHostName();
		return remoteHost;
	}

	@Override
	public int getRemotePort() {
		return req.getRemote().getPort();
	}

	@Override
	public String getLocalName() {
		if (localHost == null)
			localHost = req.getLocal().getHostString();
		return localHost;
	}

	@Override
	public String getLocalAddr() {
		if (localAddr == null)
			localAddr = getAddr(req.getLocal());
		return localAddr;
	}

	@Override
	public int getLocalPort() {
		return req.getLocal().getPort();
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		return type;
	}

	@Override
	public String getServletPath() {
		if (servletPath == null)
			servletPath = req.parseServletPath();
		return servletPath;
	}

	@Override
	public String getPathInfo() {
		if (pathInfo == null)
			pathInfo = req.parsePathInfo();
		return pathInfo == "" ? null : pathInfo;
	}

	@Override
	public String getPathTranslated() {
		return null;
	}

	@Override
	public String getContextPath() {
		return "";
	}

	@Override
	public String getRequestURI() {
		String s = getServletPath();
		return getPathInfo() == null ? s : s + getPathInfo();
	}

	@Override
	public StringBuffer getRequestURL() {
		StringBuffer append = new StringBuffer("http://").append(getServerName()).append(':').append(getServerPort()).append(getServletPath());
		if (getPathInfo() != null)
			append.append(getPathInfo());
		return append;
	}

	@Override
	public Cookie[] getCookies() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAuthType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isUserInRole(String role) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpSession getSession(boolean create) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpSession getSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String changeSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		return false;
	}

	@Deprecated
	@Override
	public boolean isRequestedSessionIdFromUrl() {
		return isRequestedSessionIdFromURL();
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void login(String username, String password) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void logout() throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		if (!getContentType().startsWith("multipart/form-data"))
			throw new ServletException("not a multipart/form-data");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Part getPart(String name) throws IOException, ServletException {
		if (!getContentType().startsWith("multipart/form-data"))
			throw new ServletException("not a multipart/form-data");
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletContext getServletContext() {
		return ctx;
	}

	private static String getAddr(InetSocketAddress a) {
		if (a == null)
			return "127.0.0.1";
		InetAddress address = a.getAddress();
		if (address == null)
			return "127.0.0.1";
		return address.getHostAddress();
	}
}