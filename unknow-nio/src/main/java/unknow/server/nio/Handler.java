/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;

/**
 * @author unknow
 */
public interface Handler {
	void onRead() throws InterruptedException, IOException;

	void onWrite() throws InterruptedException, IOException;

	boolean closed(long now, boolean close);

	void free();

	void reset();
}
