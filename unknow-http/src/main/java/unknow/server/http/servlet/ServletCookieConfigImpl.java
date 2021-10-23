/**
 * 
 */
package unknow.server.http.servlet;

import javax.servlet.SessionCookieConfig;

/**
 * @author unknow
 */
public class ServletCookieConfigImpl implements SessionCookieConfig {
	private String name;
	private String domain;
	private String path;
	private String comment;
	private boolean httpOnly;
	private boolean secure;
	private int maxAge;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getDomain() {
		return domain;
	}

	@Override
	public void setDomain(String domain) {
		this.domain = domain;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public boolean isHttpOnly() {
		return httpOnly;
	}

	@Override
	public void setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	@Override
	public int getMaxAge() {
		return maxAge;
	}

	@Override
	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}
}
