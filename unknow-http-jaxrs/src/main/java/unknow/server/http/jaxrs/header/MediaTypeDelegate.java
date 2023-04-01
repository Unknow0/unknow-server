/**
 * 
 */
package unknow.server.http.jaxrs.header;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

/**
 * @author unknow
 */
public class MediaTypeDelegate implements HeaderDelegate<MediaType> {

	public static final MediaTypeDelegate INSTANCE = new MediaTypeDelegate();

	private MediaTypeDelegate() {
	}

	@Override
	public MediaType fromString(String value) {
		int semiColonIndex = value.indexOf(';');
		int slashIndex = value.indexOf('/', semiColonIndex);
		if (slashIndex < 0)
			throw new IllegalArgumentException("invalid mediatype '" + value + "'");

		String type = value.substring(0, slashIndex);
		String subtype = value.substring(slashIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim();

		Map<String, String> parameters = new HashMap<>();
		while (semiColonIndex != -1) {
			semiColonIndex++;
			int equalSignIndex = value.indexOf('=', semiColonIndex);

			String name = value.substring(semiColonIndex, equalSignIndex).trim();
			semiColonIndex = value.indexOf(';', semiColonIndex);
			parameters.put(name, value.substring(equalSignIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim());
		}

		return new MediaType(type, subtype, parameters);
	}

	@Override
	public String toString(MediaType value) {
		StringBuilder sb = new StringBuilder(value.getType()).append('/').append(value.getSubtype());
		for (Entry<String, String> e : value.getParameters().entrySet())
			sb.append(';').append(e.getKey()).append('=').append(e.getValue());
		return sb.toString();
	}

}
