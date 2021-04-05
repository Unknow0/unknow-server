/**
 * 
 */
package unknow.server.http.servlet;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import unknow.server.http.utils.ArrayMap;

/**
 * implementation of ServletConfig
 * 
 * @author unknow
 */
public class ServletConfigImpl implements ServletConfig {
	private final String name;
	private final ServletContext context;
	private final ArrayMap<String> parameters;

	/**
	 * create new FilterConfigImpl
	 * 
	 * @param name       the name of this servlet
	 * @param context    the context
	 * @param parameters the init param
	 */
	public ServletConfigImpl(String name, ServletContext context, ArrayMap<String> parameters) {
		this.name = name;
		this.context = context;
		this.parameters = parameters;
	}

	@Override
	public String getServletName() {
		return name;
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
}
