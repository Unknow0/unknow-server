/**
 * 
 */
package unknow.server.nio.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @author unknow
 */
public class BuffersInputStream extends InputStream {
	private Buffers buffers;

	private byte[] mark;
	private int l = 0;

	public BuffersInputStream(Buffers buffers) {
		this.buffers = buffers;
	}

	@Override
	public int read() throws IOException {
		waitData();
		int b = buffers.read();
		if (mark != null && b > 0)
			mark[l++] = (byte) b;
		return b;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		waitData();
		len = buffers.read(b, off, len);
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
		buffers.prepend(mark, 0, l);
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

	private void waitData() throws IOException {
		synchronized (buffers) {
			try {
				while (buffers.length() == 0)
					buffers.wait();
			} catch (InterruptedException e) {
				throw new IOException("interrupted");
			}
		}
	}
}
