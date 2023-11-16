/**
 * 
 */
package unknow.server.nio;

/**
 * factory of handler
 * 
 * @author unknow
 */
public interface HandlerFactory {
	/** create a new handler */
	Handler create(Connection c);
}
