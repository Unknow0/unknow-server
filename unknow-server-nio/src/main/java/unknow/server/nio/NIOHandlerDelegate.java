package unknow.server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import javax.net.ssl.SSLEngine;

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
	public void prepareWrite(ByteBuffer b, long now, Consumer<ByteBuffer> c) throws IOException {
		handler.prepareWrite(b, now, c);
	}

	@Override
	public void beforeWrite(long now, Consumer<ByteBuffer> c) throws IOException {
		handler.beforeWrite(now, c);
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
	public void onOutputClosed() {
		handler.onOutputClosed();
	}

	@Override
	public boolean canClose(long now, boolean stop) {
		return handler.canClose(now, stop);
	}

	@Override
	public void startClose() {
		handler.startClose();
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
