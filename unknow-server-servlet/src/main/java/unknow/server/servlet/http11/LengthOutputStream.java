/**
 * 
 */
package unknow.server.servlet.http11;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.nio.NIOConnection.Out;
import unknow.server.servlet.impl.AbstractServletOutput;
import unknow.server.servlet.impl.ServletResponseImpl;

/**
 * http with a Content-Length (can be -1)
 * 
 * @author unknow
 */
public class LengthOutputStream extends AbstractServletOutput {
	private final Out out;
	private long length;

	/**
	 * create new LengthOutputStream
	 * 
	 * @param out    the real ouptut
	 * @param res    the servlet response
	 * @param length the length
	 */
	public LengthOutputStream(Out out, ServletResponseImpl res, long length) {
		super(res, 0);
		this.out = out;
		this.length = length;
	}

	@Override
	public void afterClose() throws IOException {
		if (length > 0)
			throw new IOException("Closing before end");
	}

	@Override
	public void write(int b) throws IOException {
		if (--length <= 0)
			throw new IOException("Extraneous data");
		super.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (len > length)
			throw new IOException("Extraneous data");
		length -= len;
		super.write(b, off, len);
	}

	@Override
	protected void writeBuffer(ByteBuffer b) throws IOException {
		out.write(b);
	}
}
