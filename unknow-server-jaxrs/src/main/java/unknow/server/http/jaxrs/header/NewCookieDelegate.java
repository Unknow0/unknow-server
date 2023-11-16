/**
 * 
 */
package unknow.server.http.jaxrs.header;

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.NewCookie.SameSite;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

/**
 * @author unknow
 */
public class NewCookieDelegate implements HeaderDelegate<NewCookie> {

	public static final NewCookieDelegate INSTANCE = new NewCookieDelegate();

	private NewCookieDelegate() {
	}

	@Override
	public NewCookie fromString(String value) {
		int semiColonIndex = value.indexOf(';');
		int i = value.indexOf('=', semiColonIndex);
		if (i < 0)
			throw new IllegalArgumentException("invalid cookie '" + value + "'");

		NewCookie.Builder b = new NewCookie.Builder(value.substring(0, i).trim());
		b.value(value.substring(i + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim());

		while (semiColonIndex != -1) {
			semiColonIndex++;
			int equalSignIndex = value.indexOf('=', semiColonIndex);

			String name = value.substring(semiColonIndex, equalSignIndex).trim();
			semiColonIndex = value.indexOf(';', semiColonIndex);

			switch (name) {
				case "Domain":
					b.domain(value.substring(equalSignIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim());
					break;
				case "Expires":
					b.expiry(DateDelegate.INSTANCE.fromString(value.substring(equalSignIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim()));
					break;
				case "HttpOnly":
					b.httpOnly(true);
					break;
				case "Max-Age":
					b.maxAge(Integer.parseInt(value.substring(equalSignIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim()));
					break;
				case "Path":
					b.path(value.substring(equalSignIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim());
					break;
				case "SameSite":
					b.sameSite(SameSite.valueOf(value.substring(equalSignIndex + 1, semiColonIndex < 0 ? value.length() : semiColonIndex).trim().toUpperCase()));
					break;
				case "Secure":
					b.secure(true);
					break;
				default:
					break;
			}
		}

		return b.build();
	}

	@Override
	public String toString(NewCookie c) {
		StringBuilder sb = new StringBuilder(c.getName()).append('=').append(c.getValue()).append(";Max-Age=").append(c.getMaxAge());
		if (c.getDomain() != null)
			sb.append(";Domain=").append(c.getDomain());
		if (c.getExpiry() != null)
			sb.append(";Expires=").append(DateDelegate.INSTANCE.toString(c.getExpiry()));
		if (c.isHttpOnly())
			sb.append(";HttpOnly");
		if (c.getPath() != null)
			sb.append(";Path=").append(c.getPath());
		if (c.getSameSite() != null)
			sb.append(";SameSite=").append(c.getSameSite());
		if (c.isSecure())
			sb.append(";Secure");
		return sb.toString();
	}

}
