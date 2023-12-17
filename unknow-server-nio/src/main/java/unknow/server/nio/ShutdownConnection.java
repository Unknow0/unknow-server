/**
 * 
 */
package unknow.server.nio;

public class ShutdownConnection extends NIOConnection {
	private final NIOServer server;

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