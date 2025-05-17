package unknow.server.servlet.http11;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOConnectionHandler;
import unknow.server.nio.NIOWorker.WorkerTask;
import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.util.io.ByteBufferInputStream;

/**
 * http/1.1 implementation
 */
public final class Http11Processor implements NIOConnectionHandler {
	private static final Logger logger = LoggerFactory.getLogger(Http11Processor.class);

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
	public void onRead(ByteBuffer b, long now) {
		if (exec.isDone())
			decode(b);
		else
			content.addBuffer(b);
	}

	@Override
	public boolean canClose(long now, boolean stop) {
		return exec.isDone() && co.keepAliveReached(now);
	}

	@Override
	public void startClose() {
		content.close();
	}

	@Override
	public boolean finishClosing(long now) {
		return exec.isDone();
	}

	private boolean decode(ByteBuffer b) {
		ServletRequestImpl req = dec.append(b);
		if (req == null)
			return false;
		content.addBuffer(b);
		exec = co.submit(new Http11Worker(this, req));
		dec.reset();
		return true;
	}

	protected void requestDone() {
		if (content.hasRemaining())
			co.execute(new NextRequest());
	}

	private final class NextRequest implements WorkerTask {

		@Override
		public void run(long now) {
			if (!exec.isDone())
				return;

			List<ByteBuffer> list = new ArrayList<>();
			content.drain(list);
			try {
				for (ByteBuffer b : list)
					co.onRead(b, now);
			} catch (IOException e) {
				logger.error("Failed to reuse content", e);
			}
		}
	}

}
