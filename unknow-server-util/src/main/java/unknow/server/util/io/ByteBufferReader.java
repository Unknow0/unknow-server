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
		decoder.decode(in.buffer(), buf, in.hasRemaining() && in.isClosed());
		buf.flip();
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
