/**
 * 
 */
package unknow.server.http.servlet.in;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import unknow.server.http.servlet.ServletResponseImpl;

/**
 * Http chuncked entity
 * 
 * @author unknow
 */
public class ChunckedInputStream extends ServletInputStream {
	private static final byte[] CRLF = new byte[] { '\r', '\n' };
	private static final byte[] END = new byte[] { '0', '\r', '\n', '\r', '\n' };
	private final InputStream out;
	private WriteListener listener;

	private byte[] buf;
	private int o = 0;

	private boolean closed;

	public ChunckedInputStream(InputStream in) {
		this.out = in;
	}
//
//	private void ensureOpen() throws IOException {
//		if (closed)
//			throw new IOException("stream closed");
//	}
//
//	@Override
//	public boolean isReady() {
//		return true;
//	}
//
//	@Override
//	public void setWriteListener(WriteListener writeListener) {
//		this.listener = writeListener;
//		if (listener != null) {
//			try {
//				listener.onWritePossible();
//			} catch (Throwable t) {
//				listener.onError(t);
//			}
//		}
//	}
//
//	@Override
//	public boolean isChuncked() {
//		return true;
//	}
//
//	@Override
//	public void close() throws IOException {
//		if (closed)
//			return;
//		flush();
//		out.write(END);
//		out.flush();
//		closed = true;
//	}
//
//	@Override
//	public void flush() throws IOException {
//		if (o == 0)
//			return;
//		res.commit();
//		out.write(Integer.toString(o, 16).getBytes());
//		out.write(CRLF);
//		out.write(buf, 0, o);
//		o = 0;
//		out.write(CRLF);
//	}
//
//	@Override
//	public int getBufferSize() {
//		return buf.length;
//	}
//
//	@Override
//	public void resetBuffers() {
//		o = 0;
//	}
//
//	@Override
//	public void setBufferSize(int size) {
//		byte[] b = new byte[size];
//		if (o > 0)
//			System.arraycopy(buf, 0, b, 0, o);
//		buf = b;
//	}
//
//	@Override
//	public void write(int b) throws IOException {
//		ensureOpen();
//		buf[o] = (byte) b;
//		writed(1);
//	}
//
//	@Override
//	public void write(byte[] b) throws IOException {
//		ensureOpen();
//		write(b, 0, b.length);
//	}
//
//	@Override
//	public void write(byte[] b, int off, int len) throws IOException {
//		ensureOpen();
//		if (o != 0) {
//			int l = Math.min(len, buf.length - o);
//			System.arraycopy(b, off, buf, o, l);
//			writed(l);
//			len -= l;
//		}
//		if (len == 0)
//			return;
//		if (len > buf.length) {
//			res.commit();
//			out.write(b, off, len);
//		} else {
//			System.arraycopy(b, off, buf, o, len);
//			writed(len);
//		}
//	}
//
//	private void writed(int i) throws IOException {
//		if ((o += i) == buf.length)
//			flush();
//	}

	@Override
	public boolean isFinished() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setReadListener(ReadListener readListener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int read() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
}
