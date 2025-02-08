package unknow.server.servlet.impl;

import java.util.Enumeration;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import unknow.server.util.data.ArrayMap;

public class ServletRequestError extends HttpServletRequestWrapper {

	private final ArrayMap<Object> attributes = new ArrayMap<>();

	public ServletRequestError(HttpServletRequest request, int sc, Throwable t) {
		super(request);
		attributes.put("javax.servlet.error.status_code", sc);
		if (t != null) {
			attributes.put("javax.servlet.error.exception_type", t.getClass());
			attributes.put("javax.servlet.error.message", t.getMessage());
			attributes.put("javax.servlet.error.exception", t);
		}
		attributes.put("javax.servlet.error.request_uri", request.getRequestURI());
		attributes.put("javax.servlet.error.servlet_name", "");
	}

	@Override
	public DispatcherType getDispatcherType() {
		return DispatcherType.ERROR;
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
		((ServletContextImpl) getServletContext()).events().fireRequestAttribute(this, name, o, old);
	}

}
