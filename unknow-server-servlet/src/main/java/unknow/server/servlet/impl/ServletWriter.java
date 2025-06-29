package unknow.server.servlet.impl;

import java.io.IOException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

public class ServletWriter extends Writer {
	private final AbstractServletOutput out;
	private final CharsetEncoder enc;

	public ServletWriter(AbstractServletOutput out, Charset c) {
		this.out = out;
		this.enc = c.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
	}

	public void write(CharBuffer cbuf) throws IOException {
		if (out.isClosed())
			throw new IOException("closed");
		CoderResult r = enc.encode(cbuf, out.buffer, false);
		if (r.isError())
			throw new IOException("Invalid caracter found");
		while (r.isOverflow()) {
			out.flush();
			r = enc.encode(cbuf, out.buffer, false);
			if (r.isError())
				throw new IOException("Invalid caracter found");
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
		write(CharBuffer.wrap(str, off, off + len));
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
		out.close();
	}
}
