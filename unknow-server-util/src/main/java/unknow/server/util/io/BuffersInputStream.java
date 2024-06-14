/**
 * 
 */
package unknow.server.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @author unknow
 */
public class BuffersInputStream extends InputStream {
	private final Buffers buffers;
	private final boolean wait;

	private long read;

	private byte[] mark;
	private int l;

	/**
	 * create a new input stream
	 * @param buffers the data to read
	 */
	public BuffersInputStream(Buffers buffers) {
		this(buffers, true);
	}

	/**
	 * create a new input stream
	 * @param buffers the data to read
	 * @param wait if false read won't wait for more data
	 */
	public BuffersInputStream(Buffers buffers, boolean wait) {
		this.buffers = buffers;
		this.wait = wait;
		this.read = 0;
		this.l = 0;
	}

	/**
	 * @return number of bytes read
	 */
	public long readCount() {
		return read;
	}

	@Override
	public int read() throws IOException {
		try {
			int b = buffers.read(wait);
			if (b > 0) {
				read++;
				if (mark != null)
					mark[l++] = (byte) b;
			}
			return b;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		try {
			len = buffers.read(b, off, len, wait);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
		if (len >= 0) {
			read += len;
			if (mark != null) {
				ensureMark(l + len);
				System.arraycopy(b, off, mark, l, len);
				l += len;
			}
		}
		return len;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public synchronized void mark(int readlimit) {
		l = 0;
		if (mark == null || mark.length < readlimit)
			mark = new byte[readlimit];
	}

	@Override
	public synchronized void reset() throws IOException {
		if (mark == null)
			throw new IOException("not marked");
		try {
			buffers.prepend(mark, 0, l);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
		mark = null;
	}

	public void writeMark(Buffers b) throws InterruptedException {
		b.write(mark, 0, l);
	}

	private void ensureMark(int len) {
		if (mark != null && mark.length < len)
			mark = Arrays.copyOf(mark, len);
	}

	@Override
	public int available() throws IOException {
		return buffers.length();
	}
}
