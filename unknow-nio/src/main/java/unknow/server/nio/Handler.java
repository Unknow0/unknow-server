/**
 * 
 */
package unknow.server.nio;

/**
 * @author unknow
 */
public interface Handler {
	void onRead();

	void onWrite();

	boolean closed(boolean close);

	void free();

	void reset();
}
