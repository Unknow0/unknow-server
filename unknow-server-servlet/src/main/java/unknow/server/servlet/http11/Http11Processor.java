package unknow.server.servlet.http11;

import java.io.IOException;
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

	private static final int START = 0;
	private static final int CONTENT = 1;
	private static final int CHUNKED_START = 2;
	private static final int CHUNKED_DATA = 3;
	private static final int CHUNKED_END = 4;
	private static final int WAIT = 5;

	private final HttpConnection co;

	private volatile Future<?> exec = CompletableFuture.completedFuture(null);

	private final ByteBufferInputStream pending;
	private final RequestDecoder dec;
	private final StringBuilder sb;

	private volatile int state = START;
	private long contentLength;
	private boolean cr;

	/**
	 * new http11 processor
	 * 
	 * @param co the connection
	 */
	public Http11Processor(HttpConnection co) {
		this.co = co;
		this.pending = new ByteBufferInputStream();
		this.dec = new RequestDecoder(this);
		this.sb = new StringBuilder();
	}

	HttpConnection co() {
		return co;
	}

	@Override
	public void onRead(ByteBuffer b, long now) {
		while (b.hasRemaining()) {
			byte c;
			switch (state) {
				case START:
					dec.reset();
					decode(b);
					break;
				case CONTENT:
					if (b.remaining() < contentLength) {
						contentLength -= b.remaining();
						dec.addContent(b);
					} else {
						state = WAIT;
						dec.addContent(b.slice().limit((int) contentLength));
						dec.closeContent();
						b.position(b.position() + (int) contentLength);
						contentLength = 0;
						break;
					}
					break;
				case CHUNKED_START:
					c = b.get();
					if (cr) {
						if (c != '\n')
							; // error
						cr = false;
						contentLength = Integer.parseInt(sb.toString(), 16);
						sb.setLength(0);
						if (contentLength == 0) {
							dec.closeContent();
							state = WAIT;
						} else
							state = CHUNKED_DATA;
						break;
					} else if (c == '\r')
						cr = true;
					else
						sb.append((char) c);
					break;
				case CHUNKED_DATA:
					if (b.remaining() < contentLength) {
						contentLength -= b.remaining();
						dec.addContent(b);
					} else {
						// read CRLF
						state = CHUNKED_END;
						dec.addContent(b.slice().limit((int) contentLength));
						b.position(b.position() + (int) contentLength);
						contentLength = 0;
						break;
					}
					break;
				case CHUNKED_END:
					c = b.get();
					if (cr) {
						if (c != '\n')
							; // error
						cr = false;
						state = CHUNKED_START;
					} else if (c == '\r')
						cr = true;
					else
						; // error
					break;
				case WAIT:
					pending.addBuffer(b);
					return;
			}
		}
	}

	@Override
	public boolean canClose(long now, boolean stop) {
		return exec.isDone() && co.keepAliveReached(now);
	}

	@Override
	public void startClose() {
		pending.close();
	}

	@Override
	public boolean finishClosing(long now) {
		return exec.isDone();
	}

	private boolean decode(ByteBuffer b) {
		ServletRequestImpl req = dec.append(b);
		if (req == null)
			return false;
		if ("chunked".equalsIgnoreCase(req.getHeader("transfer-encoding"))) {
			state = CHUNKED_START;
			contentLength = 0;
		} else {
			if ((contentLength = req.getContentLengthLong()) > 0)
				state = CONTENT;
			if (contentLength <= 0) {
				dec.closeContent();
				state = WAIT;
			}
		}
		exec = co.submit(new Http11Worker(this, req));
		return true;
	}

	protected void requestDone() {
		logger.info("{} done", this);
		state = START;

		if (pending.hasRemaining())
			co.execute(new NextRequest());
	}

	private final class NextRequest implements WorkerTask {

		@Override
		public void run(long now) {
			List<ByteBuffer> list = new ArrayList<>();
			pending.drain(list);
			try {
				for (ByteBuffer b : list)
					co.onRead(b, now);
			} catch (IOException e) {
				logger.error("Failed to reuse content", e);
			}
		}
	}

}
