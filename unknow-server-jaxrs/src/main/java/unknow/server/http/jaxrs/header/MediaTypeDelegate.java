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
		MediaType mt = fromString(value, 0, value.length());
		if (mt == null)
			throw new IllegalArgumentException("invalid mediatype '" + value + "'");

		return mt;
	}

	@Override
	public String toString(MediaType value) {
		StringBuilder sb = new StringBuilder(value.getType()).append('/').append(value.getSubtype());
		for (Entry<String, String> e : value.getParameters().entrySet())
			sb.append(';').append(e.getKey()).append('=').append(e.getValue());
		return sb.toString();
	}

	public static MediaType fromString(String value, int off, int end) {
		int semiColonIndex = value.indexOf(';', off);
		int slashIndex = value.indexOf('/', off);
		if (slashIndex < 0 || slashIndex >= end)
			return null;

		String type = value.substring(off, slashIndex).trim();
		String subtype = value.substring(slashIndex + 1, semiColonIndex > 0 && semiColonIndex < end ? semiColonIndex : end).trim();

		Map<String, String> parameters = new HashMap<>();
		while (semiColonIndex != -1 && semiColonIndex < end) {
			semiColonIndex++;
			int equalSignIndex = value.indexOf('=', semiColonIndex);
			if (equalSignIndex < 0 || equalSignIndex >= end)
				equalSignIndex = end;

			String name = value.substring(semiColonIndex, equalSignIndex).trim();
			semiColonIndex = value.indexOf(';', semiColonIndex);
			parameters.put(name, value.substring(equalSignIndex + 1, semiColonIndex >= 0 && semiColonIndex < end ? semiColonIndex : end).trim());
		}

		return new MediaType(type, subtype, parameters);
	}
}
