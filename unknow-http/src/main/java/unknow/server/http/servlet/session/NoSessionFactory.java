/**
 * 
 */
package unknow.server.http.servlet.session;

import javax.servlet.http.HttpSession;

/**
 * @author unknow
 */
public class NoSessionFactory implements SessionFactory {

	@Override
	public HttpSession get(String sessionId, boolean create) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String generateId() {
		return null;
	}

	@Override
	public void changeId(HttpSession session, String newId) {
		throw new UnsupportedOperationException();
	}
}
