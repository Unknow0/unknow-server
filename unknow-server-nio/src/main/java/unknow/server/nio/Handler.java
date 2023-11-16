/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;

/**
 * @author unknow
 */
public interface Handler {
	default void init() { // OK
	}

	void onRead() throws InterruptedException, IOException;

	void onWrite() throws InterruptedException, IOException;

	boolean closed(long now, boolean close);

	default void free() { // OK
	}
}
