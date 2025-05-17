/**
 * 
 */
package unknow.server.nio;

import javax.net.ssl.SSLEngine;

/** 
 * a connection that shutdown the server on creation
 */
public class ShutdownConnection implements NIOConnectionHandler {
	private final NIOServer server;

	/**
	 * @param server the server
	 */
	public ShutdownConnection(NIOServer server) {
		this.server = server;
	}

	@Override
	public void onInit(NIOConnection co, long now, SSLEngine sslEngine) {
		server.stop();
	}

	@Override
	public boolean canClose(long now, boolean close) {
		return true;
	}

	@Override
	public boolean finishClosing(long now) {
		return true;
	}
}