/**
 * 
 */
package unknow.server.nio.cli;

import java.io.IOException;

import unknow.server.nio.Connection;
import unknow.server.nio.Handler;
import unknow.server.nio.HandlerFactory;
import unknow.server.nio.NIOServer;

public class ShutdownHandler implements Handler, HandlerFactory {
	private final NIOServer server;

	public ShutdownHandler(NIOServer server) {
		this.server = server;
	}

	@Override
	public Handler create(Connection c) {
		return this;
	}

	@Override
	public void init() {
		server.stop();
	}

	@Override
	public void onRead() throws InterruptedException, IOException { // OK
	}

	@Override
	public void onWrite() throws InterruptedException, IOException { // OK
	}

	@Override
	public boolean closed(long now, boolean close) {
		return true;
	}

	@Override
	public void free() { // OK
	}
}