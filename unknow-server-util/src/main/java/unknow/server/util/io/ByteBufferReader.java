package unknow.server.util.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class ByteBufferReader extends Reader {
	private final ByteBufferInputStream in;
	private final CharsetDecoder decoder;
	private final CharBuffer buf;

	public ByteBufferReader(ByteBufferInputStream in, String charset) {
		this(in, Charset.forName(charset).newDecoder());
	}

	public ByteBufferReader(ByteBufferInputStream in, Charset charset) {
		this(in, charset.newDecoder());
	}

	public ByteBufferReader(ByteBufferInputStream in, CharsetDecoder decoder) {
		this.in = in;
		this.decoder = decoder;
		buf = CharBuffer.allocate(8192);
	}

	private void decode() throws IOException {
		if (buf.hasRemaining() || in.isOef())
			return;
		buf.compact();
		CoderResult r = decoder.decode(in.buffer(), buf, in.hasRemaining() && in.isClosed());
		buf.flip();
		if (r.isError())
			r.throwException();
	}

	@Override
	public int read() throws IOException {
		if (in.isOef())
			return -1;
		decode();
		return buf.get();
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (in.isOef())
			return -1;
		decode();
		len = Math.min(len, buf.remaining());
		buf.get(cbuf, off, len);
		return len;
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

}
