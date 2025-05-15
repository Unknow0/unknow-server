package unknow.server.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class ByteBufferInputStream extends InputStream {
	private static final ByteBuffer EOF = ByteBuffer.allocate(0);

	private final BlockingDeque<ByteBuffer> buffers = new LinkedBlockingDeque<>();
	private ByteBuffer current = null;

	/**
	 * add a buffer to the stream
	 * @param buffer buffer to add
	 */
	public void addBuffer(ByteBuffer buffer) {
		if (buffer != null && buffer.hasRemaining())
			buffers.offer(buffer);
	}

	public void drain(Collection<ByteBuffer> list) {
		buffers.drainTo(list);
	}

	/**
	 * get next buffer to read (wait if no buffer available)
	 * @return
	 * @throws InterruptedException
	 * @throws IOException 
	 */
	private final ByteBuffer buffer() throws IOException {
		if (current == null || current != EOF && !current.hasRemaining()) {
			try {
				current = buffers.take();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Thread interrupted while reading", e);
			}
		}
		return current;
	}

	@Override
	public int available() {
		return buffers.peek().remaining();
	}

	@Override
	public int read() throws IOException {
		ByteBuffer b = buffer();
		if (b == EOF)
			return -1;
		return b.get();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		ByteBuffer buffer = buffer();
		if (buffer == EOF)
			return -1;
		len = Math.min(len, buffer.remaining());
		buffer.get(b, off, len);
		return len;
	}

	@Override
	public void close() {
		buffers.add(EOF);
	}

	@Override
	public long skip(long n) throws IOException {
		long o = n;
		while (n > 0) {
			ByteBuffer b = buffer();
			if (b == EOF)
				return o - n;
			int r = b.remaining();
			if (n > r)
				b.position(r);
			else
				b.position(r = (int) n);
			n -= r;
		}
		return o - n;
	}

	public boolean isClosed() {
		return buffers.peek() == EOF;
	}
}
