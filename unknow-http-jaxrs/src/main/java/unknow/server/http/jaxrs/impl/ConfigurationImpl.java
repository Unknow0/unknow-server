/**
 * 
 */
package unknow.server.http.jaxrs.impl;

import java.util.Map;

import jakarta.ws.rs.SeBootstrap.Configuration;

/**
 * @author unknow
 */
public class ConfigurationImpl implements Configuration {
	private final Map<String, Object> properties;

	public ConfigurationImpl(Map<String, Object> properties) {
		this.properties = properties;
	}

	@Override
	public Object property(String name) {
		return properties.get(name);
	}

}
