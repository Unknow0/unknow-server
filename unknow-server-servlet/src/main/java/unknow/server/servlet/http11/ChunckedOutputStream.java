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
public class ChunckedOutputStream extends AbstractServletOutput {
	private static final byte[] CRLF = new byte[] { '\r', '\n' };
	private static final byte[] END = new byte[] { '0', '\r', '\n', '\r', '\n' };
	private final Out out;

	/**
	 * create new ChunckedOutputStream
	 * 
	 * @param out        the real output
	 * @param res        the servlet response (to commit)
	 */
	public ChunckedOutputStream(Out out, ServletResponseImpl res) {
		super(res, 0);
		this.out = out;
	}

	@Override
	protected void afterClose() throws IOException {
		out.write(END);
	}

	@Override
	protected void writeBuffer(ByteBuffer b) throws IOException {
		out.write(Integer.toString(b.remaining(), 16).getBytes());
		out.write(CRLF);
		out.write(b);
		out.write(CRLF);
	}
}
