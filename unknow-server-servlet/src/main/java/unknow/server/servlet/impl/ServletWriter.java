package unknow.server.servlet.impl;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import unknow.server.util.Encoder;

public class ServletWriter extends Writer {
	private final AbstractServletOutput out;
	private final Charset c;
	private final Encoder enc;

	public ServletWriter(AbstractServletOutput out, Charset c) {
		this.out = out;
		this.c = c;
		this.enc = Encoder.from(c);
	}

	public void write(CharBuffer cbuf) throws IOException {
		if (out.isClosed())
			throw new IOException("closed");

		enc.encode(cbuf, out.buffer, false);
		while (cbuf.hasRemaining()) {
			out.flush();
			enc.encode(cbuf, out.buffer, false);
		}
	}

	@Override
	public void write(int c) throws IOException {
		if (out.isClosed())
			throw new IOException("closed");
		CharBuffer cbuf = CharBuffer.allocate(1);
		cbuf.put((char) c);
		write(cbuf.flip());
	}

	@Override
	public void write(String str, int off, int len) throws IOException {
		if (out.isClosed())
			throw new IOException("closed");
		out.write(str.getBytes(c));
	}

	@Override
	public void write(char[] buf, int off, int len) throws IOException {
		if (out.isClosed())
			throw new IOException("closed");
		write(CharBuffer.wrap(buf, off, len));

	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		while (enc.flush(out.buffer))
			out.flush();
		out.close();
	}
}
