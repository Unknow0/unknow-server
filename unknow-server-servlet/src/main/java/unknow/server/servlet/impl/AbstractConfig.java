package unknow.server.servlet.impl;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

import unknow.server.util.data.ArrayMap;

public class AbstractConfig {
	protected static final String ALREADY_INITIALIZED = "already initialized";

	private final String name;
	private final ArrayMap<String> parameters;

	protected AbstractConfig(String name, ArrayMap<String> parameters) {
		this.name = name;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public String getInitParameter(String name) {
		return parameters.get(name);
	}

	public Enumeration<String> getInitParameterNames() {
		return parameters.names();
	}

	public Map<String, String> getInitParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	@SuppressWarnings("unused")
	public boolean setInitParameter(String name, String value) {
		throw new IllegalStateException(ALREADY_INITIALIZED);
	}

	@SuppressWarnings("unused")
	public Set<String> setInitParameters(Map<String, String> initParameters) {
		throw new IllegalStateException(ALREADY_INITIALIZED);
	}

}
