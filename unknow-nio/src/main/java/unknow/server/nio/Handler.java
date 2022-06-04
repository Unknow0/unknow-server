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

	boolean closed(long now, boolean close);

	void free();

	void reset();
}
