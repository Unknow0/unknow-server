package unknow.server.servlet.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

/**
 * abstract implementation of ServlerOutputStream
 */
public abstract class AbstractServletOutput extends ServletOutputStream {
	/** response that created this stream */
	protected final ServletResponseImpl res;
	/** the buffer */
	protected ByteBuffer buffer;
	private int bufferSize;

	private boolean closed;

	/**
	 * create a new AbstractServlet
	 * @param res the response
	 */
	protected AbstractServletOutput(ServletResponseImpl res) {
		this.res = res;
		if (res != null) {
			this.buffer = ByteBuffer.allocate(res.getBufferSize());
		} else {
			this.buffer = null;
			this.bufferSize = 0;
		}
		this.closed = false;
	}

	/**
	 * clear the buffer
	 */
	public final void resetBuffers() {
		buffer.clear();
	}

	/**
	 * @return the current buffer size
	 */
	public final int getBufferSize() {
		return bufferSize;
	}

	/**
	 * set the buffer size
	 * @param bufferSize the size
	 */
	public final void setBufferSize(int bufferSize) {
		if (buffer.capacity() > bufferSize || buffer.position() > 0)
			return;
		this.bufferSize = bufferSize;
		this.buffer = ByteBuffer.allocate(bufferSize);
	}

	public boolean isClosed() {
		return closed;
	}

	/**
	 * @throws IOException if the stream is closed
	 */
	protected final void ensureOpen() throws IOException {
		if (closed)
			throw new IOException("stream closed");
	}

	/**
	 * called after the stream is closed
	 * @throws IOException on error
	 */
	protected void afterClose() throws IOException { // for override
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setWriteListener(WriteListener writeListener) { // ok
	}

	@Override
	public void write(int b) throws IOException {
		ensureOpen();
		buffer.put((byte) b);
		if (!buffer.hasRemaining())
			flush();
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (len == 0)
			return;
		ensureOpen();
		int l = Math.min(len, buffer.remaining());
		buffer.put(b, off, l);
		len -= l;
		if (len == 0)
			return;
		flush();
		off += l;
		if (len >= bufferSize)
			writeBuffer(ByteBuffer.wrap(Arrays.copyOfRange(b, off, off + len)));
		else
			buffer.put(b, off, len);
	}

	@Override
	public final void close() throws IOException {
		if (closed)
			return;
		res.commit();
		closed = true;
		flush();
		afterClose();
	}

	@Override
	public final void flush() throws IOException {
		if (buffer.position() == 0)
			return;
		res.commit();
		writeBuffer(buffer.flip());
		buffer = ByteBuffer.allocate(bufferSize);
	}

	protected abstract void writeBuffer(ByteBuffer b) throws IOException;
}
