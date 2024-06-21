/**
 * 
 */
package unknow.server.servlet.http11;

import java.io.IOException;
import java.io.OutputStream;

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
	private final OutputStream out;

	/**
	 * create new ChunckedOutputStream
	 * 
	 * @param out        the real output
	 * @param res        the servlet response (to commit)
	 */
	public ChunckedOutputStream(OutputStream out, ServletResponseImpl res) {
		super(res);
		this.out = out;
	}

	@Override
	protected void afterClose() throws IOException {
		out.write(END);
	}

	@Override
	public void flush() throws IOException {
		if (buffer.isEmpty())
			return;
		res.commit();
		writeBlock();
	}

	private void writeBlock() throws IOException {
		out.write(Integer.toString(buffer.length(), 16).getBytes());
		out.write(CRLF);
		try {
			buffer.read(out, -1, false);
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		out.write(CRLF);
	}
}
