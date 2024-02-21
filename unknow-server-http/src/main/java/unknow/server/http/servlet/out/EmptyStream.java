/**
 * 
 */
package unknow.server.http.servlet.out;

import java.io.IOException;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

/**
 * @author unknow
 */
public class EmptyStream extends ServletOutputStream implements Output {
	public static final EmptyStream INSTANCE = new EmptyStream();

	private EmptyStream() {
	}

	@Override
	public boolean isChuncked() {
		return false;
	}

	@Override
	public void resetBuffers() { // OK
	}

	@Override
	public void setBufferSize(int size) {
		throw new IllegalStateException("unbuffered");
	}

	@Override
	public int getBufferSize() {
		return 0;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setWriteListener(WriteListener writeListener) { // OK
	}

	@Override
	public void write(int b) throws IOException {
		throw new IOException("empty stream");
	}

	@Override
	public void write(byte[] b) throws IOException {
		if (b.length > 0)
			throw new IOException("empty stream");
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (len > 0)
			throw new IOException("empty stream");
	}
}
