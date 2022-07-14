/**
 * 
 */
package unknow.server.http.servlet;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import unknow.server.http.data.ArrayMap;

/**
 * implementation of FilterConfig
 * 
 * @author unknow
 */
public class FilterConfigImpl implements FilterConfig, FilterRegistration {
	private final String name;
	private final Filter filter;
	private final ServletContext context;
	private final ArrayMap<String> parameters;
	private final Set<String> servletMappings;
	private final Set<String> urlMappings;
	private final Set<DispatcherType> dispatcherTypes;

	/**
	 * create new FilterConfigImpl
	 * 
	 * @param name            the name of this filter
	 * @param filter          the filter
	 * @param context         the context
	 * @param parameters      the init param
	 * @param servletMappings the servlet name mappings
	 * @param urlMappings     the url mappings
	 */
	public FilterConfigImpl(String name, Filter filter, ServletContext context, ArrayMap<String> parameters, Set<String> servletMappings, Set<String> urlMappings, Set<DispatcherType> dispatcherTypes) {
		this.name = name;
		this.filter = filter;
		this.context = context;
		this.parameters = parameters;
		this.servletMappings = servletMappings;
		this.urlMappings = urlMappings;
		this.dispatcherTypes = dispatcherTypes;
	}

	@Override
	public String getFilterName() {
		return name;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * @return the filter
	 */
	public Filter getFilter() {
		return filter;
	}

	@Override
	public String getClassName() {
		return filter.getClass().getName();
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
	public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {
		throw new IllegalStateException("already initialized");
	}

	@Override
	public Collection<String> getServletNameMappings() {
		return servletMappings;
	}

	@Override
	public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {
		throw new IllegalStateException("already initialized");
	}

	@Override
	public Collection<String> getUrlPatternMappings() {
		return urlMappings;
	}

	public Collection<DispatcherType> getDispatcherTypes() {
		return dispatcherTypes;
	}
}
