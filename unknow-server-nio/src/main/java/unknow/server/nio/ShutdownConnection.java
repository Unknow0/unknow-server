/**
 * 
 */
package unknow.server.nio;

import java.nio.channels.SelectionKey;

/** 
 * a connection that shutdown the server on creation
 */
public class ShutdownConnection extends NIOConnection {
	private final NIOServer server;

	/**
	 * @param key the selection key 
	 * @param server the server
	 */
	public ShutdownConnection(SelectionKey key, NIOServer server) {
		super(key);
		this.server = server;
	}

	@Override
	public void onInit() {
		server.stop();
	}

	@Override
	public boolean closed(long now, boolean close) {
		return true;
	}
}