/**
 * 
 */
package unknow.server.nio;

/** 
 * a connection that shutdown the server on creation
 */
public class ShutdownConnection extends NIOConnection {
	private final NIOServer server;

	/** @param server the server */
	public ShutdownConnection(NIOServer server) {
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