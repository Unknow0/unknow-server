/**
 * 
 */
package unknow.server.http.servlet;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import unknow.server.http.data.ArrayMap;

/**
 * implementation of ServletConfig
 * 
 * @author unknow
 */
public class ServletConfigImpl implements ServletConfig, ServletRegistration {
	private final String name;
	private final Servlet servlet;
	private final ServletContext context;
	private final ArrayMap<String> parameters;
	private final Set<String> mappings;

	/**
	 * create new FilterConfigImpl
	 * 
	 * @param name       the name of this servlet
	 * @param servlet    the servlet
	 * @param context    the context
	 * @param parameters the init param
	 * @param mappings   the url mappings
	 */
	public ServletConfigImpl(String name, Servlet servlet, ServletContext context, ArrayMap<String> parameters, Set<String> mappings) {
		this.name = name;
		this.servlet = servlet;
		this.context = context;
		this.parameters = parameters;
		this.mappings = mappings;
	}

	@Override
	public String getServletName() {
		return name;
	}

	/**
	 * @return the servlet
	 */
	public Servlet getServlet() {
		return servlet;
	}

	@Override
	public ServletContext getServletContext() {
		return context;
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
	public Map<String, String> getInitParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		throw new IllegalStateException("already initialized");
	}

	@Override
	public Set<String> setInitParameters(Map<String, String> initParameters) {
		throw new IllegalStateException("already initialized");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getClassName() {
		return servlet.getClass().getName();
	}

	@Override
	public Collection<String> getMappings() {
		return mappings;
	}

	@Override
	public Set<String> addMapping(String... urlPatterns) {
		throw new IllegalStateException("already initialized");
	}

	@Override
	public String getRunAsRole() {
		return null;
	}
}
