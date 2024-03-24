/**
 * 
 */
package unknow.server.servlet.impl;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import unknow.server.servlet.impl.session.SessionFactory;
import unknow.server.servlet.utils.EventManager;
import unknow.server.servlet.utils.ServletManager;
import unknow.server.util.data.ArrayMap;

/**
 * @author unknow
 */
public class ServletContextImpl implements ServletContext {
	private static final Logger logger = LoggerFactory.getLogger(ServletContextImpl.class);

	private static final String SERVER_INFO = "UnknowServer/" + ServletContextImpl.class.getPackage().getImplementationVersion();
	private final String name;
	private final String vhost;

	private final ArrayMap<String> parameters;
	private final ArrayMap<Object> attributes;

	private final ServletManager servlets;
	private final EventManager events;
	private final SessionFactory sessions;

	private final ArrayMap<String> localeEncodings;
	private final ArrayMap<String> mimeTypes;

	private String requestEncoding = "UTF8";
	private String responseEncoding = "UTF8";

	/**
	 * create new ServletContextImpl
	 * 
	 * @param name       name for this context
	 * @param parameters initial parameter
	 * @param servlets   the servlet manager
	 * @param events     the event manager
	 */
	public ServletContextImpl(String name, String vhost, ArrayMap<String> parameters, ServletManager servlets, EventManager events, SessionFactory sessions, ArrayMap<String> localeEncodings, ArrayMap<String> mimeTypes) {
		this.name = name;
		this.vhost = vhost;
		this.parameters = parameters;
		this.attributes = new ArrayMap<>();

		this.servlets = servlets;
		this.events = events;
		this.sessions = sessions;
		this.localeEncodings = localeEncodings;
		this.mimeTypes = mimeTypes;
	}

	/**
	 * @return the events manager
	 */
	public EventManager getEvents() {
		return events;
	}

	/**
	 * @return the servlet manager
	 */
	public ServletManager getServletManager() {
		return servlets;
	}

	/**
	 * @return the session factory
	 */
	public SessionFactory getSessionFactory() {
		return sessions;
	}

	/**
	 * get encoding from a locale
	 * 
	 * @param loc the locale
	 * @return the encoding to use
	 */
	public String getEncoding(Locale loc) {
		String l = loc.toLanguageTag();
		for (;;) {
			String e = localeEncodings.get(l);
			if (e != null)
				return e;
			int i = l.lastIndexOf('-');
			if (i < 0)
				return responseEncoding;
			l = l.substring(i);
		}
	}

	@Override
	public String getContextPath() {
		return "";
	}

	@Override
	public ServletContext getContext(String uripath) {
		return this;
	}

	@Override
	public int getMajorVersion() {
		return 4;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public int getEffectiveMajorVersion() {
		return 4;
	}

	@Override
	public int getEffectiveMinorVersion() {
		return 0;
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
	public void setAttribute(String name, Object object) {
		Object old = attributes.put(name, object);
		events.fireContextAttribute(this, name, object, old);
	}

	@Override
	public String getMimeType(String file) {
		int i = file.lastIndexOf('.');
		return i < 0 ? null : mimeTypes.get(file.substring(i + 1));
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		return getClassLoader().getResource(path.substring(1));
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		return getClassLoader().getResourceAsStream(path.substring(1));
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void log(String msg) {
		logger.info("{}", msg);
	}

	@Override
	public void log(String message, Throwable throwable) {
		logger.info("{}", message, throwable);
	}

	@Override
	public String getRealPath(String path) {
		return null;
	}

	@Override
	public String getServerInfo() {
		return SERVER_INFO;
	}

	@Override
	public String getInitParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return parameters.names();
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		return parameters.putOnce(name, value);
	}

	@Override
	public String getServletContextName() {
		return name;
	}

	@Override
	public Dynamic addServlet(String servletName, String className) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dynamic addServlet(String servletName, Servlet servlet) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Dynamic addJspFile(String servletName, String jspFile) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletRegistration getServletRegistration(String servletName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, String className) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FilterRegistration getFilterRegistration(String filterName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addListener(String className) {
		try {
			addListener((Class<? extends EventListener>) Class.forName(className));
		} catch (ClassNotFoundException e) {
			log("failed to load listener", e);
		}
	}

	@Override
	public <T extends EventListener> void addListener(T t) {
		events.addListener(t);
	}

	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
		try {
			addListener(createListener(listenerClass));
		} catch (ServletException e) {
			log("failed to create Listener", e);
		}
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new ServletException(e);
		}
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.getClass().getClassLoader();
	}

	@Override
	public void declareRoles(String... roleNames) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getVirtualServerName() {
		return vhost;
	}

	@Override
	public int getSessionTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSessionTimeout(int sessionTimeout) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getRequestCharacterEncoding() {
		return requestEncoding;
	}

	@Override
	public void setRequestCharacterEncoding(String encoding) {
		requestEncoding = encoding;
	}

	@Override
	public String getResponseCharacterEncoding() {
		return responseEncoding;
	}

	@Override
	public void setResponseCharacterEncoding(String encoding) {
		responseEncoding = encoding;
	}
}
