/**
 * 
 */
package unknow.server.http.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import unknow.server.http.HttpHandler;
import unknow.server.http.data.ArrayMap;
import unknow.server.http.servlet.in.ChunckedInputStream;
import unknow.server.http.servlet.in.EmptyInputStream;
import unknow.server.http.servlet.in.LengthInputStream;
import unknow.server.http.servlet.session.SessionFactory;

/**
 * @author unknow
 */
public class ServletRequestImpl implements HttpServletRequest {
	private static final Logger log = LoggerFactory.getLogger(ServletRequestImpl.class);

	private static final Cookie[] EMPTY = new Cookie[0];

	private final ArrayMap<Object> attributes = new ArrayMap<>();

	private final ServletContextImpl ctx;
	private final HttpHandler req;
	private final DispatcherType type;
	private final ServletResponseImpl res;

	private final List<String> path;
	private int pathInfoIndex;

	private String protocol = null;
	private String method = null;
	private String servletPath = null;
	private String pathInfo = null;
	private String query = null;
	private Map<String, List<String>> queryParam = null;

	private String encoding = null;
	private long contentLength = -2;

	private String sessionFromCookie;
	private String sessionFromUrl;

	private Map<String, List<String>> headers;
	private Map<String, String[]> parameter;

	private String remoteAddr;
	private String remoteHost;
	private String localAddr;
	private String localHost;

	private HttpSession session;

	private Cookie[] cookies = EMPTY;

	private List<Locale> locales;

	private BufferedReader reader;
	private ServletInputStream input;

	private final InetSocketAddress remote;
	private final InetSocketAddress local;

	/**
	 * create new ServletRequestImpl
	 * 
	 * @param ctx  the context
	 * @param req  the raw request
	 * @param type dispatcher type of this request
	 * @param res  the response
	 */
	public ServletRequestImpl(ServletContextImpl ctx, HttpHandler req, DispatcherType type, ServletResponseImpl res) {
		this.ctx = ctx;
		this.req = req;
		this.type = type;
		this.res = res;
		this.path = new ArrayList<>();

		this.headers = new HashMap<>();

		this.remote = null;// req != null ? req.getRemote() : null;
		this.local = null;// req != null ? req.getLocal() : null;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public void setQueryParam(Map<String, List<String>> query) {
		this.queryParam = query;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = headers;
	}

	public List<String> getPaths() {
		return path;
	}

	public void addPath(String path) {
		this.path.add(path);
	}

	public void setPathInfo(int index) {
		pathInfoIndex = index;
	}

	private void parseParam() {
		if (parameter != null)
			return;
		parameter = new HashMap<>();
		Map<String, List<String>> p = new HashMap<>(queryParam);

		try {
			if ("POST".equals(getMethod()) && "application/x-www-form-urlencoded".equalsIgnoreCase(getContentType()))
				parseContentParam(p);
		} catch (IOException e) {
			log.error("failed to parse params from content", e);
		}

		String[] s = new String[0];
		for (Entry<String, List<String>> e : p.entrySet())
			parameter.put(e.getKey(), e.getValue().toArray(s));
	}

	/**
	 * @param p
	 * @throws IOException
	 */
	private void parseContentParam(Map<String, List<String>> p) throws IOException {
		BufferedReader r = getReader();
		int c;
		String key;
		StringBuilder sb = new StringBuilder();
		do {
			c = readParam(sb, r, true);
			key = sb.toString();
			sb.setLength(0);
			if (c == '=')
				c = readParam(sb, r, false);

			List<String> list = p.get(key);
			if (list == null)
				p.put(key, list = new ArrayList<>(1));
			list.add(sb.toString());
			sb.setLength(0);
		} while (c != -1);
	}

	private int readParam(StringBuilder sb, Reader r, boolean key) throws IOException {
		int c;
		while ((c = r.read()) != -1) {
			if (c == '&' || key && c == '=')
				return c;
			if (c == '%') // TODO decode
				;
			sb.append((char) c);
		}
		return c;
	}

	/**
	 * @param servletPath the servletPath to set
	 */
	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
		this.pathInfo = "";
		this.query = "";
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
		Object old = attributes.put(name, o);
		ctx.getEvents().fireRequestAttribute(this, name, o, old);
	}

	@Override
	public String getCharacterEncoding() {
		if (encoding == null) {
			String header = getHeader("content-type");
			if (header != null) {
				int i = header.indexOf(";encoding=");
				if (i > 0) {
					int e = header.indexOf(';', i);
					if (e < 0)
						e = header.length();
					encoding = header.substring(i + 10, e);
				}
			}
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
		List<String> list = headers.get(name.toLowerCase());
		return list == null || list.isEmpty() ? null : list.get(0);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		List<String> list = headers.get(name.toLowerCase());
		return list == null || list.isEmpty() ? Collections.emptyEnumeration() : Collections.enumeration(list);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
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
		return getLocales().nextElement();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		if (locales == null) {
			locales = new ArrayList<>();
			Enumeration<String> langs = getHeaders("Accept-Language");
			while (langs.hasMoreElements()) {
				String l = langs.nextElement();
				int i = l.indexOf(';');
				if (i > 0) // TODO support quality ?
					l = l.substring(0, i);
				if ("*".equals(l))
					continue;
				locales.add(Locale.forLanguageTag(l));
			}
			if (locales.isEmpty())
				locales.add(Locale.getDefault());
		}
		return Collections.enumeration(locales);
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
		if (reader != null)
			throw new IllegalStateException("getReader() called");
		if (input == null)
			input = createInput();
		return input;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (input != null)
			throw new IllegalStateException("getInputStream() called");
		if (reader == null)
			reader = new BufferedReader(new InputStreamReader(createInput(), getCharacterEncoding()));
		return reader;
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
		return strings == null ? null : strings[0];
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
		return method;
	}

	@Override
	public String getQueryString() {
		return query.isEmpty() ? null : query;
	}

	@Override
	public String getProtocol() {
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

	@Override
	public String getRemoteAddr() {
		if (remoteAddr == null)
			remoteAddr = getAddr(remote);
		return remoteAddr;
	}

	@Override
	public String getRemoteHost() {
		if (remoteHost == null)
			remoteHost = remote.getHostName();
		return remoteHost;
	}

	@Override
	public int getRemotePort() {
		return remote.getPort();
	}

	@Override
	public String getLocalName() {
		if (localHost == null)
			localHost = local.getHostString();
		return localHost;
	}

	@Override
	public String getLocalAddr() {
		if (localAddr == null)
			localAddr = getAddr(local);
		return localAddr;
	}

	@Override
	public int getLocalPort() {
		return local.getPort();
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
		if (servletPath == null) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < pathInfoIndex; i++)
				sb.append('/').append(path.get(i));
			servletPath = sb.length() == 0 ? "/" : sb.toString();
		}
		return servletPath;
	}

	@Override
	public String getPathInfo() {
		if (pathInfo == null) {
			StringBuilder sb = new StringBuilder();
			for (int i = pathInfoIndex; i < path.size(); i++)
				sb.append('/').append(path.get(i));
			pathInfo = sb.toString();
		}
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
		StringBuffer append = new StringBuffer(getScheme()).append("://").append(getServerName()).append(':').append(getServerPort()).append(getServletPath());
		if (getPathInfo() != null)
			append.append(getPathInfo());
		return append;
	}

	@Override
	public Cookie[] getCookies() {
		return cookies.length == 0 ? null : cookies;
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
		if (sessionFromCookie != null)
			return sessionFromCookie;
		if (sessionFromUrl != null)
			return sessionFromUrl;
		ctx.getEffectiveSessionTrackingModes(); // TODO manage other session traking mode
		SessionCookieConfig cookieCfg = ctx.getSessionCookieConfig();
		if (cookieCfg != null) {
			String name = cookieCfg.getName();
			Cookie[] c = getCookies();
			for (int i = 0; i < c.length; i++) {
				if (name.equals(c[i].getName()))
					return sessionFromCookie = c[i].getValue();
			}
		}
		// else look in url
		return null;
	}

	@Override
	public HttpSession getSession(boolean create) {
		if (session != null)
			return session;
		String sessionId = getRequestedSessionId();
		SessionFactory sessionFactory = ctx.getSessionFactory();
//		ctx.getSessionCookieConfig().
		if (sessionId == null && create) {
			sessionId = sessionFactory.generateId();
			res.addCookie(new Cookie("JSESSIONID", sessionId)); // TODO add httpOnly
		}
		return session = sessionFactory.get(sessionId, create);
	}

	@Override
	public HttpSession getSession() {
		return getSession(true);
	}

	@Override
	public String changeSessionId() {
		if (session == null)
			throw new IllegalStateException("no session");
		SessionFactory sessionFactory = ctx.getSessionFactory();
		String newId = sessionFactory.generateId();
		sessionFactory.changeId(session, newId);
		return newId;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return getSession(false) != null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return sessionFromCookie != null;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return sessionFromUrl != null;
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

	private ServletInputStream createInput() {
		String tr = getHeader("transfer-encoding");
		if ("chunked".equalsIgnoreCase(tr))
			return new ChunckedInputStream(req.getIn());
		long l = getContentLengthLong();
		if (l > 0)
			return new LengthInputStream(req.getIn(), l);
		return EmptyInputStream.INSTANCE;
	}

	private static String getAddr(InetSocketAddress a) {
		if (a == null)
			return "127.0.0.1";
		InetAddress address = a.getAddress();
		if (address == null)
			return "127.0.0.1";
		return address.getHostAddress();
	}

	@Override
	public String getRequestId() { // TODO add value for keep-alive connection
		return getServletConnection().getConnectionId();
	}

	@Override
	public String getProtocolRequestId() {
		return getServletConnection().getProtocolConnectionId();
	}

	@Override
	public ServletConnection getServletConnection() {
		// TODO Auto-generated method stub
		return null;
	}
}