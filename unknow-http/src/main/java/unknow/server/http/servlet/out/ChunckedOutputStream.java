/**
 * 
 */
package unknow.server.http.servlet.out;

import java.io.IOException;
import java.io.OutputStream;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import unknow.server.http.servlet.ServletResponseImpl;

/**
 * Http chuncked entity
 * 
 * @author unknow
 */
public class ChunckedOutputStream extends ServletOutputStream implements Output {
	private static final byte[] CRLF = new byte[] { '\r', '\n' };
	private static final byte[] END = new byte[] { '0', '\r', '\n', '\r', '\n' };
	private final OutputStream out;
	private final ServletResponseImpl res;
	private WriteListener listener;

	private byte[] buf;
	private int o = 0;

	private boolean closed;

	/**
	 * create new ChunckedOutputStream
	 * 
	 * @param out        the real output
	 * @param res        the servlet response (to commit)
	 * @param bufferSize the inital buffer size
	 */
	public ChunckedOutputStream(OutputStream out, ServletResponseImpl res, int bufferSize) {
		this.out = out;
		this.res = res;
		if (bufferSize < 4096)
			bufferSize = 4096;
		buf = new byte[bufferSize];
		closed = false;
	}

	private void ensureOpen() throws IOException {
		if (closed)
			throw new IOException("stream closed");
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {
		this.listener = writeListener;
		if (listener != null) {
			try {
				listener.onWritePossible();
			} catch (Throwable t) {
				listener.onError(t);
			}
		}
	}

	@Override
	public boolean isChuncked() {
		return true;
	}

	@Override
	public void close() throws IOException {
		if (closed)
			return;
		flush();
		out.write(END);
		out.flush();
		closed = true;
	}

	@Override
	public void flush() throws IOException {
		if (o == 0)
			return;
		res.commit();
		out.write(Integer.toString(o, 16).getBytes());
		out.write(CRLF);
		out.write(buf, 0, o);
		o = 0;
		out.write(CRLF);
	}

	@Override
	public int getBufferSize() {
		return buf.length;
	}

	@Override
	public void resetBuffers() {
		o = 0;
	}

	@Override
	public void setBufferSize(int size) {
		byte[] b = new byte[size];
		if (o > 0)
			System.arraycopy(buf, 0, b, 0, o);
		buf = b;
	}

	@Override
	public void write(int b) throws IOException {
		ensureOpen();
		buf[o] = (byte) b;
		writed(1);
	}

	@Override
	public void write(byte[] b) throws IOException {
		ensureOpen();
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ensureOpen();
		if (o != 0) {
			int l = Math.min(len, buf.length - o);
			System.arraycopy(b, off, buf, o, l);
			writed(l);
			len -= l;
		}
		if (len == 0)
			return;
		if (len > buf.length) {
			res.commit();
			out.write(b, off, len);
		} else {
			System.arraycopy(b, off, buf, o, len);
			writed(len);
		}
	}

	private void writed(int i) throws IOException {
		if ((o += i) == buf.length)
			flush();
	}
}
