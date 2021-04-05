/**
 * 
 */
package unknow.server.http.servlet.out;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import javax.servlet.ServletOutputStream;

public final class ServletWriter<T extends ServletOutputStream & Output> extends Writer implements Output {
	private final T out;
	private final CharsetEncoder enc;
	private final byte[] buf;
	private final CharBuffer cb;
	private final ByteBuffer bb;

	public ServletWriter(T out, Charset charset) {
		this.out = out;
		this.enc = charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);

		this.buf = new byte[4096];
		this.cb = CharBuffer.allocate(2048);
		this.bb = ByteBuffer.wrap(buf);
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		while (len > 0) {
			int l = Math.min(len, cb.remaining());
			cb.put(cbuf, off, l);
			cb.flip();
			enc.encode(cb, bb, true);
			out.write(buf, 0, bb.position());
			bb.clear();
			off += l;
			len -= l;
			if (cb.remaining() > 0)
				cb.compact();
			else
				cb.clear();
		}
		if (cb.position() > 0) {
			cb.flip();
			while (cb.remaining() > 0) {
				enc.encode(cb, bb, true);
				out.write(buf, 0, bb.position());
				bb.clear();
			}
		}
	}

	@Override
	public void write(char[] cbuf) throws IOException {
		write(cbuf, 0, cbuf.length);
	}

	@Override
	public void write(int c) throws IOException {
		cb.put((char) c);
		cb.flip();
		enc.encode(cb, bb, true);
		out.write(buf, 0, bb.position());
		cb.clear();
		bb.clear();
	}

	@Override
	public void write(String str) throws IOException {
		write(str, 0, str.length());
	}

	@Override
	public void write(String str, int off, int len) throws IOException {
		while (len > 0) {
			int l = Math.min(len, cb.remaining());
			cb.put(str, off, l);
			cb.flip();
			enc.encode(cb, bb, true);
			out.write(buf, 0, bb.position());
			bb.clear();
			off += l;
			len -= l;
			if (cb.remaining() > 0)
				cb.compact();
			else
				cb.clear();
		}
		if (cb.position() > 0) {
			cb.flip();
			while (cb.remaining() > 0) {
				enc.encode(cb, bb, true);
				out.write(buf, 0, bb.position());
				bb.clear();
			}
		}
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	@Override
	public boolean isChuncked() {
		return out.isChuncked();
	}

	@Override
	public void resetBuffers() {
		out.resetBuffers();
	}

	@Override
	public void setBufferSize(int size) {
		out.setBufferSize(size);
	}

	@Override
	public int getBufferSize() {
		return out.getBufferSize();
	}
}