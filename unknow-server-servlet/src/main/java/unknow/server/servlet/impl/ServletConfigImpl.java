/**
 * 
 */
package unknow.server.servlet.impl;

import java.util.Collection;
import java.util.Set;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import unknow.server.util.data.ArrayMap;

/**
 * implementation of ServletConfig
 * 
 * @author unknow
 */
public class ServletConfigImpl extends AbstractConfig implements ServletConfig, ServletRegistration {

	private final Servlet servlet;
	private final ServletContext context;
	private final Set<String> mappings;

	/**
	 * create new FilterConfigImpl
	 * 
	 * @param name       the name of this servlet
	 * @param context    the context
	 * @param parameters the init param
	 * @param mappings   the url mappings
	 */
	public ServletConfigImpl(String name, Servlet servlet, ServletContext context, ArrayMap<String> parameters, Set<String> mappings) {
		super(name, parameters);
		this.servlet = servlet;
		this.context = context;
		this.mappings = mappings;
	}

	@Override
	public String getServletName() {
		return getName();
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
	public String getClassName() {
		return servlet.getClass().getName();
	}

	@Override
	public Collection<String> getMappings() {
		return mappings;
	}

	@Override
	public Set<String> addMapping(String... urlPatterns) {
		throw new IllegalStateException(ALREADY_INITIALIZED);
	}

	@Override
	public String getRunAsRole() {
		return null;
	}
}
