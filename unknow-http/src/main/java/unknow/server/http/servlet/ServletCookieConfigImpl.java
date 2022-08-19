/**
 * 
 */
package unknow.server.http.servlet;

import java.util.Collections;
import java.util.Map;

import jakarta.servlet.SessionCookieConfig;
import unknow.server.http.data.ArrayMap;

/**
 * @author unknow
 */
public class ServletCookieConfigImpl implements SessionCookieConfig {
	public static final String NAME = "name";
	public static final String DOMAIN = "domain";
	public static final String PATH = "path";
	public static final String HTTP_ONLY = "http-only";
	public static final String SECURE = "secure";
	public static final String MAX_AGE = "max-age";

	private final Map<String, String> attributes = new ArrayMap<>();

	@Override
	public String getName() {
		return getAttribute(NAME);
	}

	@Override
	public void setName(String name) {
		setAttribute(NAME, name);
	}

	@Override
	public String getDomain() {
		return getAttribute(DOMAIN);
	}

	@Override
	public void setDomain(String domain) {
		setAttribute(DOMAIN, domain);
	}

	@Override
	public String getPath() {
		return getAttribute(PATH);
	}

	@Override
	public void setPath(String path) {
		setAttribute(PATH, path);
	}

	@Deprecated
	@Override
	public String getComment() {
		return null;
	}

	@Deprecated
	@Override
	public void setComment(String comment) {
	}

	@Override
	public boolean isHttpOnly() {
		return Boolean.parseBoolean(getAttribute(HTTP_ONLY));
	}

	@Override
	public void setHttpOnly(boolean httpOnly) {
		setAttribute(HTTP_ONLY, Boolean.toString(httpOnly));
	}

	@Override
	public boolean isSecure() {
		return Boolean.parseBoolean(getAttribute(SECURE));
	}

	@Override
	public void setSecure(boolean secure) {
		setAttribute(SECURE, Boolean.toString(secure));
	}

	@Override
	public int getMaxAge() {
		String s = getAttribute(MAX_AGE);
		return s == null ? -1 : Integer.parseInt(s);
	}

	@Override
	public void setMaxAge(int maxAge) {
		setAttribute(MAX_AGE, Integer.toString(maxAge));
	}

	@Override
	public void setAttribute(String name, String value) {
		attributes.put(name.toLowerCase(), value);
	}

	@Override
	public String getAttribute(String name) {
		return attributes.get(name.toLowerCase());
	}

	@Override
	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}
}
