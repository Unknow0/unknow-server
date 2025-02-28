package unknow.server.servlet.impl;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ServletWriter extends Writer {
	private final AbstractServletOutput<?> out;
	private final Charset charset;

	public ServletWriter(AbstractServletOutput<?> out, Charset charset) {
		this.out = out;
		this.charset = charset;
	}

	@Override
	public void write(int c) throws IOException {
		if (charset == StandardCharsets.UTF_8) {
			if (c <= 0x7F) { // ASCII (1 byte)
				out.write((byte) c);
			} else if (c <= 0x7FF) { // 2-byte UTF-8
				out.write((byte) (0xC0 | (c >> 6)));
				out.write((byte) (0x80 | (c & 0x3F)));
			} else { // 3-byte UTF-8 (valide pour BMP, UTF-16 basique)
				out.write((byte) (0xE0 | (c >> 12)));
				out.write((byte) (0x80 | ((c >> 6) & 0x3F)));
				out.write((byte) (0x80 | (c & 0x3F)));
			}
		} else if (charset == StandardCharsets.US_ASCII)
			out.write(c);
		else
			write(Character.toString(c));
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		out.write(new String(cbuf, off, len).getBytes(charset));
	}

	@Override
	public void write(String str, int off, int len) throws IOException {
		out.write(str.substring(off, off + len).getBytes(charset));
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
