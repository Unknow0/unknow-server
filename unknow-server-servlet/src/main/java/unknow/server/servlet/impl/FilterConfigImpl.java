/**
 * 
 */
package unknow.server.servlet.impl;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import unknow.server.util.data.ArrayMap;

/**
 * implementation of FilterConfig
 * 
 * @author unknow
 */
public class FilterConfigImpl extends AbstractConfig implements FilterConfig, FilterRegistration {

	private final Filter filter;
	private final ServletContext context;
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
	 * @param dispatcherTypes the allowed dispatcher types
	 */
	public FilterConfigImpl(String name, Filter filter, ServletContext context, ArrayMap<String> parameters, Set<String> servletMappings, Set<String> urlMappings,
			Set<DispatcherType> dispatcherTypes) {
		super(name, parameters);
		this.filter = filter;
		this.context = context;
		this.servletMappings = servletMappings;
		this.urlMappings = urlMappings;
		this.dispatcherTypes = dispatcherTypes;
	}

	@Override
	public String getFilterName() {
		return getName();
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
	public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {
		throw new IllegalStateException(ALREADY_INITIALIZED);
	}

	@Override
	public Collection<String> getServletNameMappings() {
		return servletMappings;
	}

	@Override
	public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {
		throw new IllegalStateException(ALREADY_INITIALIZED);
	}

	@Override
	public Collection<String> getUrlPatternMappings() {
		return urlMappings;
	}

	/**
	 * @return the allowed dispacher types
	 */
	public Collection<DispatcherType> getDispatcherTypes() {
		return dispatcherTypes;
	}
}
