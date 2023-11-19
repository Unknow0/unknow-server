/**
 * 
 */
package unknow.server.nio.cli;

import unknow.server.nio.NIOConnection;
import unknow.server.nio.NIOServer;
import unknow.server.util.pool.Pool;

public class ShutdownConnection extends NIOConnection {
	private final NIOServer server;

	public ShutdownConnection(Pool<NIOConnection> pool, NIOServer server) {
		super(pool);
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