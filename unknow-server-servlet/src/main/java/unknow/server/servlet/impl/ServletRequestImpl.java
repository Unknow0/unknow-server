/**
 * 
 */
package unknow.server.servlet.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.Principal;
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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import unknow.server.servlet.impl.session.SessionFactory;
import unknow.server.servlet.utils.PathUtils;
import unknow.server.util.data.ArrayMap;

/**
 * @author unknow
 */
public abstract class ServletRequestImpl implements HttpServletRequest {
	private static final Logger logger = LoggerFactory.getLogger(ServletRequestImpl.class);

	private static final String[] EMPTYSTR = new String[0];
	private static final Cookie[] EMPTY = new Cookie[0];

	private final ArrayMap<Object> attributes = new ArrayMap<>();

	private final ServletContextImpl ctx;

	protected final String scheme;
	protected final String method;
	protected final String path;
	protected final String queryString;
	protected final String protocol;

	private final InetSocketAddress remote;
	private final InetSocketAddress local;

	private final ServletInputStreamImpl rawInput;

	private Map<String, String[]> parameters;
	private String encoding;
	private int pathInfo;
	private List<Locale> locales;
	private HttpSession session;
	private Cookie[] cookies = EMPTY;

	private BufferedReader reader;
	private ServletInputStream input;

	/**
	 * create new ServletRequestImpl
	 * 
	 * @param ctx the servlet context
	 * @param scheme url scheme
	 * @param method http method
	 * @param uri the uri (with queryPath)
	 * @param protocol the http protocol
	 * @param remote the report address
	 * @param local the local address
	 */
	public ServletRequestImpl(ServletContextImpl ctx, String scheme, String method, String uri, String protocol, InetSocketAddress remote, InetSocketAddress local) {
		this.ctx = ctx;
		this.scheme = scheme;
		this.method = method;

		int i = uri.indexOf('?');
		this.path = i < 0 ? uri : uri.substring(0, i);
		this.queryString = i < 0 ? null : uri.substring(i + 1);
		this.protocol = protocol;
		this.remote = remote;
		this.local = local;

		this.rawInput = new ServletInputStreamImpl();
	}

	public void setPathInfo(int index) {
		pathInfo = index;
	}

	private void parseParam() {
		if (parameters != null)
			return;

		Map<String, List<String>> map = new HashMap<>();
		String str = getQueryString();
		if (str != null) {
			try (StringReader r = new StringReader(str)) {
				PathUtils.pathQuery(r, map);
			} catch (IOException e) {
				logger.error("failed to parse params from content", e);
			}
		}

		if ("POST".equals(getMethod()) && "application/x-www-form-urlencoded".equalsIgnoreCase(getContentType())) {
			try (BufferedReader r = new BufferedReader(new InputStreamReader(rawInput, getCharacterEncoding()))) {
				PathUtils.pathQuery(r, map);
			} catch (IOException e) {
				logger.error("failed to parse params from content", e);
			}
		}

		if (!map.isEmpty()) {
			parameters = new HashMap<>();
			for (Entry<String, List<String>> e : map.entrySet())
				parameters.put(e.getKey(), e.getValue().toArray(EMPTYSTR));
		} else
			parameters = Collections.emptyMap();

	}

	public ServletInputStreamImpl rawInput() {
		return rawInput;
	}

	@Override
	public abstract String getHeader(String name);

	@Override
	public abstract Enumeration<String> getHeaders(String name);

	@Override
	public abstract Enumeration<String> getHeaderNames();

	@Override
	public abstract long getDateHeader(String name);

	@Override
	public abstract int getIntHeader(String name);

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
		ctx.events().fireRequestAttribute(this, name, o, old);
	}

	@Override
	public String getCharacterEncoding() {
		if (encoding != null)
			return encoding;
		String header = getHeader("content-type");
		if (header != null) {
			int i = header.indexOf(";encoding=");
			if (i > 0) {
				int e = header.indexOf(';', i);
				if (e < 0)
					e = header.length();
				return encoding = header.substring(i + 10, e);
			}
		}
		return encoding = ctx.getRequestCharacterEncoding();
	}

	@Override
	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
		encoding = env;
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
	public abstract long getContentLengthLong();

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (reader != null)
			throw new IllegalStateException("getReader() called");
		if (input == null)
			input = rawInput();
		return input;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		if (input != null)
			throw new IllegalStateException("getInputStream() called");
		if (reader == null)
			reader = new BufferedReader(new InputStreamReader(rawInput(), getCharacterEncoding()));
		return reader;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		if (parameters == null)
			parseParam();
		return Collections.enumeration(parameters.keySet());
	}

	@Override
	public String[] getParameterValues(String name) {
		if (parameters == null)
			parseParam();
		return parameters.get(name);
	}

	@Override
	public String getParameter(String name) {
		if (parameters == null)
			parseParam();
		String[] strings = parameters.get(name);
		return strings == null ? null : strings[0];
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		if (parameters == null)
			parseParam();
		return Collections.unmodifiableMap(parameters);
	}

	@Override
	public String getScheme() {
		return scheme;
	}

	@Override
	public String getMethod() {
		return method;
	}

	@Override
	public String getQueryString() {
		return queryString;
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
		return getAddr(remote);
	}

	@Override
	public String getRemoteHost() {
		return remote.getHostString();
	}

	@Override
	public int getRemotePort() {
		return remote.getPort();
	}

	@Override
	public String getLocalName() {
		return local.getHostString();
	}

	@Override
	public String getLocalAddr() {
		return getAddr(local);
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
		return DispatcherType.REQUEST;
	}

	@Override
	public String getServletPath() {
		return path.substring(0, pathInfo);
	}

	@Override
	public String getPathInfo() {
		String substring = path.substring(pathInfo);
		return substring.isEmpty() ? null : substring;
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
		return path;
	}

	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer(getScheme()).append("://").append(getServerName()).append(':').append(getServerPort()).append(getRequestURI());
	}

	@Override
	public Cookie[] getCookies() {
		// TODO parse
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
//		if (sessionFromCookie != null)
//			return sessionFromCookie;
//		if (sessionFromUrl != null)
//			return sessionFromUrl;
//		co.ctx().getEffectiveSessionTrackingModes(); // TODO manage other session traking mode
//		SessionCookieConfig cookieCfg = co.ctx().getSessionCookieConfig();
//		if (cookieCfg != null) {
//			String name = cookieCfg.getName();
//			Cookie[] c = getCookies();
//			for (int i = 0; i < c.length; i++) {
//				if (name.equals(c[i].getName()))
//					return sessionFromCookie = c[i].getValue();
//			}
//		}
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
//			res.addCookie(new Cookie("JSESSIONID", sessionId)); // TODO add httpOnly
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
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
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
		return Collections.emptyList();
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

	private static String getAddr(InetSocketAddress a) {
		if (a == null)
			return "127.0.0.1";
		InetAddress address = a.getAddress();
		if (address == null)
			return "127.0.0.1";
		return address.getHostAddress();
	}
}