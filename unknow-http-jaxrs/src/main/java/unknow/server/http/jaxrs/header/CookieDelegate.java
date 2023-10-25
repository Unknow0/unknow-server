/**
 * 
 */
package unknow.server.http.jaxrs.header;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

/**
 * @author unknow
 */
public class CookieDelegate implements HeaderDelegate<Cookie> {

	public static final CookieDelegate INSTANCE = new CookieDelegate();

	private CookieDelegate() {
	}

	@Override
	public Cookie fromString(String value) {
		int semiColonIndex = value.indexOf(';');
		int i = value.indexOf('=', semiColonIndex);
		if (i < 0)
			throw new IllegalArgumentException("invalid cookie '" + value + "'");

		int version = 0;
		String n = null;
		String v = null;
		String path = null;
		String domain = null;

		while (semiColonIndex != -1) {
			semiColonIndex++;
			int equalSignIndex = value.indexOf('=', semiColonIndex);

			String name = value.substring(semiColonIndex, equalSignIndex).trim();
			semiColonIndex = value.indexOf(';', semiColonIndex);

			switch (name) {
				case "$Domain":
					domain = value.substring(equalSignIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim();
					break;
				case "$Version":
					version = Integer.parseInt(value.substring(equalSignIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim());
					break;
				case "$Path":
					path = value.substring(equalSignIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim();
					break;
				default:
					n = name;
					v = value.substring(equalSignIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim();
					break;
			}
		}
		return new Cookie.Builder(n).value(v).path(path).domain(domain).version(version).build();
	}

	@Override
	public String toString(Cookie c) {
		StringBuilder sb = new StringBuilder(c.getName()).append('=').append(c.getValue());
		if (c.getDomain() != null)
			sb.append(";Domain=").append(c.getDomain());
		if (c.getPath() != null)
			sb.append(";Path=").append(c.getPath());
		return sb.toString();
	}

}
