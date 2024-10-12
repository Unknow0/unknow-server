/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;

import javax.net.ssl.SSLEngine;

import unknow.server.util.io.Buffers;

/** 
 * a connection that shutdown the server on creation
 */
public class ShutdownConnection implements NIOConnectionHandler {
	private final NIOServer server;

	/**
	 * @param key the selection key 
	 * @param server the server
	 */
	public ShutdownConnection(NIOServer server) {
		this.server = server;
	}

	@Override
	public void onInit(NIOConnectionAbstract co, SSLEngine sslEngine) {
		server.stop();
	}

	@Override
	public boolean closed(long now, boolean close) {
		return true;
	}

	@Override
	public void onHandshakeDone(SSLEngine sslEngine) throws InterruptedException { // ok
	}

	@Override
	public void onRead(Buffers b) throws InterruptedException, IOException { // ok
	}

	@Override
	public void onWrite() throws InterruptedException, IOException { // ok
	}

	@Override
	public void onFree() throws IOException { // ok
	}
}