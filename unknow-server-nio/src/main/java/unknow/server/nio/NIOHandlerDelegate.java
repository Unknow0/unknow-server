package unknow.server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;

import unknow.server.util.io.ByteBuffers;

public class NIOHandlerDelegate implements NIOConnectionHandler {
	protected final NIOConnectionHandler handler;

	protected NIOHandlerDelegate(NIOConnectionHandler handler) {
		this.handler = handler;
	}

	@Override
	public boolean asyncInit() {
		return handler.asyncInit();
	}

	@Override
	public void init(NIOConnection co, long now, SSLEngine sslEngine) throws IOException {
		handler.init(co, now, sslEngine);
	}

	@Override
	public void onHandshakeDone(SSLEngine sslEngine, long now) throws IOException {
		handler.onHandshakeDone(sslEngine, now);
	}

	@Override
	public void onRead(ByteBuffer b, long now) throws IOException {
		handler.onRead(b, now);
	}

	@Override
	public void transformWrite(ByteBuffers buffers, long now) throws IOException {
		handler.transformWrite(buffers, now);
	}

	@Override
	public boolean hasPendingWrites() {
		return handler.hasPendingWrites();
	}

	@Override
	public void onWrite(long now) throws IOException {
		handler.onWrite(now);
	}

	@Override
	public boolean canClose(long now, boolean stop) {
		return handler.canClose(now, stop);
	}

	@Override
	public void startClose(long now) {
		handler.startClose(now);
	}

	@Override
	public boolean finishClosing(long now) {
		return handler.finishClosing(now);
	}

	@Override
	public void doneClosing() {
		handler.doneClosing();
	}
}
