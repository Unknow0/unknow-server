/**
 * 
 */
package unknow.server.http.servlet.session;

import jakarta.servlet.http.HttpSession;

/**
 * @author unknow
 */
public interface SessionFactory {
	/**
	 * get an existing session or create a new one if create
	 * 
	 * @param sessionId the session to retrieve
	 * @param create    if true create a new session if sessionId not found
	 * @return the session or null if not found with create false
	 */
	HttpSession get(String sessionId, boolean create);

	/**
	 * generate a new sessionId
	 * 
	 * @return a new sessionId
	 */
	String generateId();

	/**
	 * change the id of a session
	 * 
	 * @param session the session to change
	 * @param newId   the new session id
	 */
	void changeId(HttpSession session, String newId);
}