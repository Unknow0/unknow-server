/**
 * 
 */
package unknow.server.servlet.impl.in;

import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * Http chuncked entity
 * 
 * @author unknow
 */
public class ChunckedInputStream extends ServletInputStream {
	private static final String MALFORMED_CHUNKED_STREAM = "malformed chunked stream";

	private static final int START = 0;
	private static final int CHUNK = 1;
	private static final int DONE = 2;

	private final InputStream in;

	private ReadListener listener;

	private int step = 0;
	private int chunkLen;
	private final byte[] b = new byte[4096];
	private int l;
	private int o;

	private final StringBuilder sb = new StringBuilder();

	/**
	 * @param in raw input
	 */
	public ChunckedInputStream(InputStream in) {
		this.in = in;
	}

	@Override
	public boolean isFinished() {
		return step == DONE;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public long skip(long n) throws IOException {
		long s = 0;

		while (!isFinished() && n > l) {
			s += l;
			l = 0;
			ensureData();
		}
		if (l > n) {
			s += n;
			o += n;
			l -= n;
		}
		return s;
	}

	@Override
	public void setReadListener(ReadListener listener) {
		this.listener = listener;
	}

	private void readData() throws IOException {
		o = 0;
		l = in.read(b, 0, Math.min(b.length, chunkLen));
		chunkLen -= l;
		if (chunkLen == 0) {
			step = START;
			if (in.read() != '\r')
				throw new IOException(MALFORMED_CHUNKED_STREAM);
			if (in.read() != '\n')
				throw new IOException(MALFORMED_CHUNKED_STREAM);
		}
	}

	private void ensureData() throws IOException {
		if (o < l || step == DONE)
			return;
		if (step == CHUNK) {
			readData();
			return;
		}
		for (;;) {
			int c = in.read();
			if (c == -1)
				throw new IOException("connection reset by peer");
			if (c == '\r') {
				c = in.read();
				if (c != '\n')
					throw new IOException(MALFORMED_CHUNKED_STREAM);
				break;
			}
			sb.append((char) c);
		}
		chunkLen = Integer.parseInt(sb.toString(), 16);
		sb.setLength(0);
		if (chunkLen == 0) {
			step = DONE;
			return;
		}
		step = CHUNK;
		readData();
	}

	@Override
	public int read() throws IOException {
		ensureData();
		return step == DONE ? -1 : b[o++] & 0xFF;
	}

	@Override
	public int read(byte[] buf, int off, int len) throws IOException {
		ensureData();
		if (step == DONE)
			return -1;
		len = Math.min(l, len);
		System.arraycopy(b, o, buf, off, len);
		o += len;
		return len;
	}
}
