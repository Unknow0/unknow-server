package unknow.server.servlet.http11;

import java.nio.ByteBuffer;

import unknow.server.nio.NIOConnectionHandler;
import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.utils.OrderedLock;

/**
 * http/1.1 implementation
 */
public final class Http11Processor implements NIOConnectionHandler {
	private static final int START = 0;
	private static final int CONTENT = 1;
	private static final int CHUNKED_START = 2;
	private static final int CHUNKED_DATA = 3;
	private static final int CHUNKED_END = 4;

	private final HttpConnection co;
	private final OrderedLock lock;

	private final RequestDecoder dec;
	private final StringBuilder sb;

	private int state = START;
	private long contentLength;
	private boolean cr;

	/**
	 * new http11 processor
	 * 
	 * @param co the connection
	 */
	public Http11Processor(HttpConnection co) {
		this.co = co;
		this.lock = new OrderedLock();
		this.dec = new RequestDecoder(this);
		this.sb = new StringBuilder();
	}

	HttpConnection co() {
		return co;
	}

	@Override
	public void onRead(ByteBuffer b, long now) {
		while (b.hasRemaining())
			process(b);
	}

	private final void process(ByteBuffer b) {
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
					state = START;
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
						state = CHUNKED_END;
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
					dec.addContent(b.slice());
					b.position(b.limit());
				} else {
					// read CRLF
					state = CHUNKED_END;
					dec.addContent(b.slice().limit((int) contentLength));
					b.position(b.position() + (int) contentLength);
					contentLength = 0;
				}
				break;
			case CHUNKED_END:
				c = b.get();
				if (cr) {
					if (c != '\n')
						; // error
					cr = false;
					state = dec.closed() ? START : CHUNKED_START;
				} else if (c == '\r')
					cr = true;
				else
					; // error
				break;
			default:
		}
	}

	@Override
	public boolean canClose(long now, boolean stop) {
		return lock.isDone() && co.keepAliveReached(now);
	}

	@Override
	public boolean finishClosing(long now) {
		return lock.isDone();
	}

	private void decode(ByteBuffer b) {
		ServletRequestImpl req = dec.append(b);
		if (req == null)
			return;
		if ("chunked".equalsIgnoreCase(req.getHeader("transfer-encoding"))) {
			state = CHUNKED_START;
			contentLength = 0;
		} else if ((contentLength = req.getContentLengthLong()) > 0)
			state = CONTENT;
		else {
			dec.closeContent();
			state = START;
		}
		co.submit(new Http11Worker(this, req, lock.nextId()));
	}

	protected void waitUntil(int reqId) throws InterruptedException {
		lock.waitUntil(reqId);
	}

	protected void requestDone() {
		lock.unlockNext();
	}
}
