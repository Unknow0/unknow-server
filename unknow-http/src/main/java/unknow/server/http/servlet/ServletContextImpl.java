/**
 * 
 */
package unknow.server.http.servlet;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import unknow.server.http.utils.ArrayMap;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.ServletManager;

/**
 * @author unknow
 */
public class ServletContextImpl implements ServletContext {
	private final String name;

	private final ArrayMap<String> parameters;
	private final ArrayMap<Object> attributes;

	private final ServletManager servlets;
	private final EventManager events;

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
	public ServletContextImpl(String name, ArrayMap<String> parameters, ServletManager servlets, EventManager events) {
		this.name = name;
		this.parameters = parameters;
		this.attributes = new ArrayMap<>();

		this.servlets = servlets;
		this.events = events;
	}

	/**
	 * @return the events manager
	 */
	public EventManager getEvents() {
		return events;
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
		Object old = attributes.set(name, object);
		events.fireContextAttribute(this, name, object, old);
	}

	@Override
	public String getMimeType(String file) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
	}

	@Deprecated
	@Override
	public void log(Exception exception, String msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void log(String message, Throwable throwable) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getRealPath(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServerInfo() {
		// TODO Auto-generated method stub
		return null;
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
		return parameters.setOnce(name, value);
	}

	@Override
	public String getServletContextName() {
		return name;
	}

	@Deprecated
	@Override
	public Servlet getServlet(String name) throws ServletException {
		Servlet[] servlets = this.servlets.getServlets();
		for (int i = 0; i < servlets.length; i++) {
			Servlet s = servlets[i];
			if (name.equals(s.getServletConfig().getServletName()))
				return s;
		}
		return null;
	}

	@Deprecated
	@Override
	public Enumeration<Servlet> getServlets() {
		return new Enumeration<Servlet>() {
			private final Servlet[] s = servlets.getServlets();
			private int i = 0;

			@Override
			public boolean hasMoreElements() {
				return i < s.length;
			}

			@Override
			public Servlet nextElement() {
				return s[i++];
			}
		};
	}

	@Deprecated
	@Override
	public Enumeration<String> getServletNames() {
		return new Enumeration<String>() {
			private final Servlet[] s = servlets.getServlets();
			private int i = 0;

			@Override
			public boolean hasMoreElements() {
				return i < s.length;
			}

			@Override
			public String nextElement() {
				return s[i++].getServletConfig().getServletName();
			}
		};
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
	public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
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
			return clazz.newInstance();
		} catch (IllegalAccessException | InstantiationException e) {
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
		// TODO Auto-generated method stub
		return null;
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
