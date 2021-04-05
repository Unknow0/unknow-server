/**
 * 
 */
package unknow.server.http.servlet.out;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

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
	public void resetBuffers() {
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
	public void setWriteListener(WriteListener writeListener) {
	}

	@Override
	public void write(int b) throws IOException {
	}
}
