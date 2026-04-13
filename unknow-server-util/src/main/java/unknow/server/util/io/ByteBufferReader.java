package unknow.server.util.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import unknow.server.util.Decoder;

public class ByteBufferReader extends Reader {
	private final ByteBufferInputStream in;
	private final Decoder decoder;
	private final CharBuffer buf;

	public ByteBufferReader(ByteBufferInputStream in, String charset) {
		this(in, Charset.forName(charset));
	}

	public ByteBufferReader(ByteBufferInputStream in, Charset charset) {
		this.in = in;
		this.decoder = Decoder.from(charset);
		buf = CharBuffer.allocate(8192).flip();
	}

	private void decode() throws IOException {
		if (buf.hasRemaining() || in.isOef())
			return;
		buf.compact();
		boolean end = in.hasRemaining() && in.isClosed();
		if (!end || in.buffer().hasRemaining())
			decoder.decode(in.buffer(), buf, end);
		else
			decoder.flush(buf);
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
		decode();
		if (!buf.hasRemaining())
			return -1;
		len = Math.min(len, buf.remaining());
		buf.get(cbuf, off, len);
		return len;
	}

	@Override
	public int read(CharBuffer target) throws IOException {
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
		return n;
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

}
