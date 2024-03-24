/**
 * 
 */
package unknow.server.servlet.http11;

import java.io.IOException;
import java.io.OutputStream;

import unknow.server.servlet.impl.ServletResponseImpl;
import unknow.server.servlet.impl.out.AbstractServletOutput;

/**
 * http with a Content-Length (can be -1)
 * 
 * @author unknow
 */
public class LengthOutputStream extends AbstractServletOutput {
	private final OutputStream out;
	private long length;

	/**
	 * create new LengthOutputStream
	 * 
	 * @param out    the real ouptut
	 * @param res    the servlet response
	 * @param length the length
	 */
	public LengthOutputStream(OutputStream out, ServletResponseImpl res, long length) {
		super(res);
		this.out = out;
		this.length = length;
	}

	@Override
	public void afterClose() throws IOException {
		if (length > 0) {
			byte[] b = new byte[4096];
			while (length > 0) {
				int l = (int) Math.min(length, 4096);
				out.write(b, 0, l);
				length -= l;
			}
		}
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
	public void flush() throws IOException {
		res.commit();
		try {
			buffer.read(out, -1, false);
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
