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
 * Http chuncked entity
 * 
 * @author unknow
 */
public class Http11OutputStream extends AbstractServletOutput {
	private static final byte[] CRLF = new byte[] { '\r', '\n' };
	private static final byte[] END = new byte[] { '0', '\r', '\n', '\r', '\n' };
	private final Out out;
	private long length;

	/**
	 * create new ChunckedOutputStream
	 * 
	 * @param out        the real output
	 * @param res        the servlet response (to commit)
	 */
	public Http11OutputStream(Out out, ServletResponseImpl res) {
		super(res, 0);
		this.out = out;
		this.length = res.getContentLength();
	}

	public void setLength(long length) {
		this.length = length;
	}

	public boolean isChunked() {
		return length < 0;
	}

	@Override
	protected void afterClose() throws IOException {
		if (length < 0)
			out.write(END);
		else if (length > 0)
			throw new IOException("Missing data");
	}

	@Override
	protected void writeBuffer(ByteBuffer b) throws IOException {
		if (length < 0) { // chuncked encoding
			out.write(Integer.toString(b.remaining(), 16).getBytes());
			out.write(CRLF);
			out.write(b);
			out.write(CRLF);
			return;
		}
		if (length < b.remaining())
			throw new IOException("extraneous data");
		length -= b.remaining();
		out.write(b);
	}
}
