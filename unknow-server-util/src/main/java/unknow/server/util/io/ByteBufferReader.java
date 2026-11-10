package unknow.server.util.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

import unknow.server.util.Decoder;

public class ByteBufferReader extends Reader {
	private final ByteBufferInputStream in;
	private final Decoder decoder;
	private final CharBuffer buf;
	private boolean flushed;

	public ByteBufferReader(ByteBufferInputStream in, String charset) {
		this(in, Charset.forName(charset));
	}

	public ByteBufferReader(ByteBufferInputStream in, Charset charset) {
		this.in = in;
		this.decoder = Decoder.from(charset);
		buf = CharBuffer.allocate(8192).flip();
		flushed = false;
	}

	private void decode() throws IOException {
		if (buf.hasRemaining())
			return;
		buf.compact();
		if (in.isEof()) {
			if (!flushed)
				flushed = !decoder.flush(buf.compact());
		} else {
			ByteBuffer b = in.buffer();
			if (b != null)
				decoder.decode(b, buf, in.isLast());
			else
				flushed = !decoder.flush(buf);
		}
		buf.flip();
	}

	@Override
	public int read() throws IOException {
		decode();
		if (!buf.hasRemaining())
			return -1;
		return buf.get();
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		Objects.checkFromIndexSize(off, len, cbuf.length);
		if (len == 0)
			return 0;
		decode();
		if (!buf.hasRemaining())
			return -1;
		len = Math.min(len, buf.remaining());
		buf.get(cbuf, off, len);
		return len;
	}

	@Override
	public int read(CharBuffer target) throws IOException {
		if (target.isReadOnly())
			throw new IOException("Target should be writable");
		if (!target.hasRemaining())
			return 0;
		decode();
		if (!buf.hasRemaining())
			return -1;
		int l = buf.remaining();
		int r = target.remaining();
		if (r > l) {
			target.put(buf);
			return l;
		}
		l = buf.limit();
		target.put(buf.limit(buf.position() + r));
		buf.limit(l);
		return r;
	}

	@Override
	public long skip(long n) throws IOException {
		decode();
		if (!buf.hasRemaining())
			return 0;
		int l = (int) Math.min(n, buf.remaining());
		buf.position(buf.position() + l);
		return l;
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

}
