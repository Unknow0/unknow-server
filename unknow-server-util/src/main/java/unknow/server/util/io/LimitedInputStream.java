package unknow.server.util.io;

import java.io.IOException;
import java.io.InputStream;

public final class LimitedInputStream extends InputStream {
	private final InputStream in;
	private int limit;

	/**
	 * create new LimitedInputStream
	 * 
	 * @param in source
	 */
	public LimitedInputStream(InputStream in) {
		this.in = in;
	}

	/**
	 * @param limit the limit to set
	 */
	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public int read() throws IOException {
		if (limit == 0)
			return -1;
		limit--;
		return in.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (len > limit)
			len = limit;
		limit -= len;
		return in.read(b, off, len);
	}

	@Override
	public int available() throws IOException {
		int a = in.available();
		return a < limit ? a : limit;
	}

	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}

	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
	}
}