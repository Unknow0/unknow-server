package unknow.server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;

public class NIOHandlerDelegate implements NIOConnectionHandler {
	protected final NIOConnectionHandler handler;

	protected NIOHandlerDelegate(NIOConnectionHandler handler) {
		this.handler = handler;
	}

	@Override
	public void onInit(NIOConnection co, long now, SSLEngine sslEngine) throws IOException {
		handler.onInit(co, now, sslEngine);
	}

	@Override
	public void onHandshakeDone(SSLEngine sslEngine) throws IOException {
		handler.onHandshakeDone(sslEngine);
	}

	@Override
	public void onRead(ByteBuffer b, long now) throws IOException {
		handler.onRead(b, now);
	}

	@Override
	public void onWrite(long now) throws IOException {
		handler.onWrite(now);
	}

	@Override
	public boolean closed(long now, boolean stop) {
		return handler.closed(now, stop);
	}

	@Override
	public void onFree() throws IOException {
		handler.onFree();
	}

}
