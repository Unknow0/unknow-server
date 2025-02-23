package unknow.server.servlet.impl;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

public class ServletWriter extends Writer {
	private final AbstractServletOutput<?> out;
	private final Charset charset;

	public ServletWriter(AbstractServletOutput<?> out, Charset charset) {
		this.out = out;
		this.charset = charset;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		out.buffer.writeCharSequence(new CharArray(cbuf, off, len), charset);
	}

	@Override
	public void write(String str) throws IOException {
		out.buffer.writeCharSequence(str, charset);
	}

	@Override
	public void write(String str, int off, int len) throws IOException {
		out.buffer.writeCharSequence(str.substring(off, off + len), charset);
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	private static class CharArray implements CharSequence {
		private final char[] cbuf;
		private int off;
		private int len;

		public CharArray(char[] cbuf, int off, int len) {
			this.cbuf = cbuf;
			this.off = off;
			this.len = len;
		}

		@Override
		public int length() {
			return len;
		}

		@Override
		public char charAt(int index) {
			return cbuf[off + index];
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			if (start < 0 || end > len || end < start)
				throw new IndexOutOfBoundsException();
			if (start == 0 && end == len)
				return this;
			return new CharArray(cbuf, off + start, end - start);
		}
	}
}
