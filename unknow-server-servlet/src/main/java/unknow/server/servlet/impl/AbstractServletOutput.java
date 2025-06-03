package unknow.server.servlet.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

/**
 * abstract implementation of ServlerOutputStream
 */
public abstract class AbstractServletOutput extends ServletOutputStream {
	/** response that created this stream */
	protected final ServletResponseImpl res;
	private final int position;
	/** the buffer */
	ByteBuffer buffer;
	private int bufferSize;

	private boolean closed;

	/**
	 * create a new AbstractServlet
	 * @param res the response
	 * @param position bytebuffer position (free space in the start)
	 */
	protected AbstractServletOutput(ServletResponseImpl res, int position) {
		this.res = res;
		this.position = position;
		if (res != null) {
			this.buffer = ByteBuffer.allocate(res.getBufferSize() + position).position(position);
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
		if (buffer != null)
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
		this.buffer = ByteBuffer.allocate(bufferSize + position).position(position);
	}

	public int size() {
		return buffer.position();
	}

	public boolean isClosed() {
		return closed;
	}

	public boolean isEmpty() {
		return buffer == null || buffer.position() == position;
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
		if (len >= bufferSize) {
			ByteBuffer buf = ByteBuffer.allocate(len + position).position(position + len);
			System.arraycopy(b, off, buf.array(), position, len);
			writeBuffer(buf);
		} else
			buffer.put(b, off, len);
	}

	@Override
	public final void close() throws IOException {
		if (closed)
			return;
		closed = true;
		res.commit();
		flush();
		afterClose();
	}

	@Override
	public final void flush() throws IOException {
		if (isEmpty())
			return;
		res.commit();
		writeBuffer(buffer.flip().position(position));
		buffer = ByteBuffer.allocate(bufferSize + position).position(position);
	}

	protected abstract void writeBuffer(ByteBuffer b) throws IOException;
}
