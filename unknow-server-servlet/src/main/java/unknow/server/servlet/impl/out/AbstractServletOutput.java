package unknow.server.servlet.impl.out;

import java.io.IOException;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import unknow.server.servlet.impl.ServletResponseImpl;
import unknow.server.util.io.Buffers;

/**
 * abstract implementation of ServlerOutputStream
 */
public abstract class AbstractServletOutput extends ServletOutputStream {
	/** the buffer */
	protected final Buffers buffer;
	/** response taht created this stream */
	protected final ServletResponseImpl res;
	private int bufferSize;

	private boolean closed;

	/**
	 * create a new AbstractServlet
	 * @param res the response
	 */
	protected AbstractServletOutput(ServletResponseImpl res) {
		this.res = res;
		if (res != null) {
			this.buffer = new Buffers();
			setBufferSize(res.getBufferSize());
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
		if (!buffer.isEmpty())
			throw new IllegalStateException("data already written");

		int r = bufferSize % Buffers.BUF_LEN; // make size a multiple of chunk size
		this.bufferSize = r == 0 ? bufferSize : bufferSize + Buffers.BUF_LEN - r;
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
		try {
			buffer.write(b);
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (buffer.length() >= bufferSize)
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
		try {
			buffer.write(b, off, len);
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		if (buffer.length() >= bufferSize)
			flush();
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
