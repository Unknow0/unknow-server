/**
 * 
 */
package unknow.server.http.servlet.out;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import unknow.server.http.servlet.ServletResponseImpl;

/**
 * http with a Content-Length (can be -1)
 * 
 * @author unknow
 */
public class LengthOutputStream extends ServletOutputStream implements Output {
	private final OutputStream out;
	private final ServletResponseImpl res;
	private WriteListener listener;
	private long length;

	/**
	 * create new LengthOutputStream
	 * 
	 * @param out    the real ouptut
	 * @param res    the servlet response
	 * @param length the length
	 */
	public LengthOutputStream(OutputStream out, ServletResponseImpl res, long length) {
		this.out = out;
		this.res = res;
		this.length = length;
	}

	private void ensureOpen() throws IOException {
		if (length == 0)
			throw new IOException("stream closed");

	}

	@Override
	public boolean isChuncked() {
		return false;
	}

	@Override
	public void close() throws IOException {
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
	public void resetBuffers() {
	}

	@Override
	public void setBufferSize(int size) {
		throw new IllegalArgumentException("not buffered");
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
		listener = writeListener;
		if (listener != null) {
			try {
				listener.onWritePossible();
			} catch (Throwable t) {
				listener.onError(t);
			}
		}
	}

	@Override
	public void write(int b) throws IOException {
		ensureOpen();
		res.commit();
		out.write(b);
		length--;
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ensureOpen();
		len = (int) Math.min(len, length);
		res.commit();
		out.write(b, off, len);
		length -= len;
	}

}
