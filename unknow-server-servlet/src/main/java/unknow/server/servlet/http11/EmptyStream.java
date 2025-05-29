/**
 * 
 */
package unknow.server.servlet.http11;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.servlet.impl.AbstractServletOutput;

/**
 * @author unknow
 */
public class EmptyStream extends AbstractServletOutput {
	public static final EmptyStream INSTANCE = new EmptyStream();

	private EmptyStream() {
		super(null, 0);
	}

	@Override
	public void write(int b) throws IOException {
		throw new IOException("content lenth = 0");
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		throw new IOException("content lenth = 0");
	}

	@Override
	protected void writeBuffer(ByteBuffer b) throws IOException { // ok
	}
}
