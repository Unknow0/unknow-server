/**
 * 
 */
package unknow.server.servlet.http11;

import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * @author unknow
 */
public class LengthInputStream extends ServletInputStream {
	private final InputStream in;
	private long length;

	/**
	 * create new ServletInputLength
	 */
	public LengthInputStream(InputStream in, long length) {
		this.in = in;
		this.length = length;
	}

	@Override
	public boolean isFinished() {
		return length == 0;
	}

	@Override
	public long skip(long n) throws IOException {
		long skip = in.skip(Math.min(n, length));
		length -= skip;
		return skip;
	}

	@Override
	public boolean isReady() {
		try {
			return in.available() > 0;
		} catch (@SuppressWarnings("unused") IOException e) {
			return true;
		}
	}

	@Override
	public void setReadListener(ReadListener readListener) { // ok
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (length == 0)
			return -1;
		int i = in.read(b, 0, length > len ? len : (int) length);
		if (i > 0)
			length -= i;
		return i;
	}

	@Override
	public int read() throws IOException {
		if (length == 0)
			return -1;
		int i = in.read();
		if (i >= 0)
			length--;
		return i;
	}
}
