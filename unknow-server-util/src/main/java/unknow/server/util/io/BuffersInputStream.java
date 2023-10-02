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

	private byte[] mark;
	private int l = 0;

	public BuffersInputStream(Buffers buffers) {
		this.buffers = buffers;
	}

	@Override
	public int read() throws IOException {
		try {
			int b = buffers.read(true);
			if (mark != null && b > 0)
				mark[l++] = (byte) b;
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
			len = buffers.read(b, off, len, true);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
		if (mark != null && len >= 0) {
			ensureMark(l + len);
			System.arraycopy(b, off, mark, l, len);
			l += len;
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

	private void ensureMark(int len) {
		if (mark != null && mark.length < len)
			mark = Arrays.copyOf(mark, len);
	}

	@Override
	public int available() throws IOException {
		return buffers.length();
	}
}
