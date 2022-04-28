/**
 * 
 */
package unknow.server.http.servlet.in;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

/**
 * @author unknow
 */
public class LengthInputStream extends ServletInputStream {
	private final InputStream in;
	private long length;
	private ReadListener listener;

	/**
	 * create new ServletInputLength
	 */
	public LengthInputStream(InputStream in, long length) {
		this.in = in;
		this.length = length;
	}

	@Override
	public boolean isFinished() {
		return length > 0;
	}

	@Override
	public boolean isReady() {
		try {
			return in.available() > 0;
		} catch (IOException e) {
			return true;
		}
	}

	@Override
	public void setReadListener(ReadListener readListener) {
		this.listener = readListener;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (length == 0)
			return -1;
		int i = in.read(b, 0, b.length > length ? (int) length : b.length);
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
