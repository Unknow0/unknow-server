/**
 * 
 */
package unknow.server.http.servlet.in;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

/**
 * @author unknow
 */
public class EmptyInputStream extends ServletInputStream {
	/** the instance */
	public static final ServletInputStream INSTANCE = new EmptyInputStream();

	private EmptyInputStream() {
	}

	@Override
	public boolean isFinished() {
		return true;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setReadListener(ReadListener readListener) {
		try {
			readListener.onAllDataRead();
		} catch (IOException e) {
		}
	}

	@Override
	public int available() throws IOException {
		return 0;
	}

	@Override
	public byte[] readAllBytes() throws IOException {
		return new byte[0];
	}

	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		return -1;
	}

	@Override
	public byte[] readNBytes(int len) throws IOException {
		return new byte[0];
	}

	@Override
	public int readLine(byte[] b, int off, int len) throws IOException {
		return -1;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return -1;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return -1;
	}

	@Override
	public int read() throws IOException {
		return -1;
	}

	@Override
	public long transferTo(OutputStream out) throws IOException {
		return 0;
	}
}
