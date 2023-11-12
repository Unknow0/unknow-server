/**
 * 
 */
package unknow.server.http.jaxrs.header;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

/**
 * @author unknow
 */
public class EntityTagDelegate implements HeaderDelegate<EntityTag> {

	public static final EntityTagDelegate INSTANCE = new EntityTagDelegate();

	private static final String WEAK_PREFIX = "W/";

	@Override
	public EntityTag fromString(String header) {
		if (header == null)
			throw new IllegalArgumentException("ETag value can not be null");

		if ("*".equals(header))
			return new EntityTag("*");

		String tag = header.trim();
		// See please https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag for weak validator
		if (tag.startsWith(WEAK_PREFIX))
			return new EntityTag(tag.length() > 2 ? tag.substring(2) : "", true);

		if (tag.length() > 0 && !tag.startsWith("\"") && !tag.endsWith("\""))
			return new EntityTag(tag, false);
		if (tag.length() < 2 || !tag.startsWith("\"") || !tag.endsWith("\""))
			throw new IllegalArgumentException("Misformatted ETag : " + header);
		tag = tag.length() == 2 ? "" : tag.substring(1, tag.length() - 1);
		return new EntityTag(tag, false);
	}

	@Override
	public String toString(EntityTag tag) {
		StringBuilder sb = new StringBuilder();
		if (tag.isWeak())
			sb.append(WEAK_PREFIX);

		String tagValue = tag.getValue();
		if (!tagValue.startsWith("\""))
			sb.append('"').append(tagValue).append('"');
		else
			sb.append(tagValue);
		return sb.toString();
	}
}
