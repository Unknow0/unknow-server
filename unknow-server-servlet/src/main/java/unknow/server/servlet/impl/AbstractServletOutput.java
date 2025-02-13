package unknow.server.servlet.impl;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import unknow.server.util.io.Buffers;

/**
 * abstract implementation of ServlerOutputStream
 */
public abstract class AbstractServletOutput<T extends ServletResponseImpl> extends ServletOutputStream {
	protected final ChannelHandlerContext out;
	/** response that created this stream */
	protected final T res;

	/** the buffer */
	protected final ByteBuf buffer;
	private int bufferSize;

	private boolean closed;

	/**
	 * create a new AbstractServlet
	 * @param res the response
	 */
	protected AbstractServletOutput(ChannelHandlerContext out, T res) {
		this.out = out;
		this.res = res;
		if (res != null) {
			bufferSize = res.getBufferSize();
			this.buffer = Unpooled.buffer(bufferSize < 8192 ? 8192 : bufferSize);
		} else {
			this.buffer = null;
			this.bufferSize = 0;
		}
		this.closed = false;
	}

	protected abstract void writebuffer() throws IOException;

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

	public int remaingSize() {
		return buffer.readableBytes();
	}

	/**
	 * set the buffer size
	 * @param bufferSize the size
	 */
	public final void setBufferSize(int bufferSize) {
		if (buffer.writerIndex() > 0)
			throw new IllegalStateException("data already written");

		int r = bufferSize % Buffers.BUF_LEN; // make size a multiple of chunk size
		this.bufferSize = r == 0 ? bufferSize : bufferSize + Buffers.BUF_LEN - r;
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
		buffer.writeByte(b);
		if (buffer.writerIndex() >= bufferSize)
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
		buffer.writeBytes(b, off, len);
		if (buffer.writerIndex() >= bufferSize)
			flush();
	}

	@Override
	public final void flush() throws IOException {
		if (buffer.readableBytes() == 0)
			return;
		try {
			res.commit();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
		writebuffer();
	}

	@Override
	public final void close() throws IOException {
		if (closed)
			return;
		closed = true;
		flush();
		afterClose();
	}
}
