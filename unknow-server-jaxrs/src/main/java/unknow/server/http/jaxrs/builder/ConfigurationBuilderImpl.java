/**
 * 
 */
package unknow.server.http.jaxrs.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import jakarta.ws.rs.SeBootstrap.Configuration;
import jakarta.ws.rs.SeBootstrap.Configuration.Builder;
import unknow.server.http.jaxrs.impl.ConfigurationImpl;

/**
 * @author unknow
 */
public class ConfigurationBuilderImpl implements Configuration.Builder {
	private final Map<String, Object> properties = new HashMap<>();

	@Override
	public Configuration build() {
		return new ConfigurationImpl(new HashMap<>(properties));
	}

	@Override
	public Builder property(String name, Object value) {
		properties.put(name, value);
		return this;
	}

	@Override
	public <T> Builder from(BiFunction<String, Class<T>, Optional<T>> propertiesProvider) {
		// TODO
		return this;
	}

}
