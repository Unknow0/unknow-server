/**
 * 
 */
package unknow.server.nio.cli;

import unknow.server.nio.NIOConnection;
import unknow.server.nio.NIOServer;

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