package unknow.server.servlet.http11;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import unknow.server.nio.NIOConnectionHandler;
import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.util.io.ByteBufferInputStream;

/**
 * http/1.1 implementation
 */
public final class Http11Processor implements NIOConnectionHandler {

	private final HttpConnection co;

	private volatile Future<?> exec = CompletableFuture.completedFuture(null);

	private final ByteBufferInputStream content;
	private final RequestDecoder dec;

	/**
	 * new http11 processor
	 * 
	 * @param co the connection
	 */
	public Http11Processor(HttpConnection co) {
		this.co = co;
		this.content = new ByteBufferInputStream();
		this.dec = new RequestDecoder(this);
	}

	HttpConnection co() {
		return co;
	}

	InputStream content() {
		return content;
	}

	@Override
	public void onRead(ByteBuffer b, long now) throws IOException {
		if (b == null) {
			content.close();
			return;
		}
		if (exec.isDone()) {
			ServletRequestImpl req = dec.append(b);
			if (req != null) {
				content.addBuffer(b);
				exec = co.submit(new Http11Worker(co, req));
				dec.reset();
			}
		} else
			content.addBuffer(b);
	}

	@Override
	public boolean closed(long now, boolean stop) {
		if (!exec.isDone())
			return false;
		if (content.isClosed())
			return true;
		return co.keepAliveReached(now);
	}

	@Override
	public final void onFree() {
		exec.cancel(true);
	}
}
