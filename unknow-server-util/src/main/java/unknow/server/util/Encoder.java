package unknow.server.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public interface Encoder {
	static final byte[] REPL = { (byte) 0xEF, (byte) 0xBF, (byte) 0xBD };

	/**
	 * get encoder from a charset
	 * @param charset the charset
	 * @return the encoder
	 */
	public static Encoder from(Charset charset) {
		if (charset.equals(StandardCharsets.UTF_8))
			return new Utf8Encoder();
		return new DefaultEncoder(charset.newEncoder().replaceWith(REPL).onMalformedInput(CodingErrorAction.REPLACE));
	}

	/**
	 * append one byte
	 * @param cbuf char to encode
	 * @param bbuf byte output
	 * @param endOfInput the data is done
	 */
	void encode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput);

	/**
	 * flush remaining bytes
	 * @param bbuf output
	 * @return true if we need to recall flush with more space
	 */
	boolean flush(ByteBuffer bbuf);

	public class DefaultEncoder implements Encoder {
		private final CharsetEncoder enc;
		private final CharBuffer c;
		private boolean endCalled;

		public DefaultEncoder(CharsetEncoder dec) {
			this.enc = dec;
			this.c = CharBuffer.allocate(4096);
			this.endCalled = false;
		}

		@Override
		public void encode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput) {
			int l = cbuf.limit();
			while (cbuf.position() < l) {
				c.put(cbuf.limit(Math.min(l, cbuf.position() + c.remaining())));
				CoderResult r = enc.encode(c.flip(), bbuf, endOfInput);
				c.compact();
				cbuf.limit(l);
				if (endOfInput)
					endCalled = true;
				if (r.isOverflow())
					return;
				if (r.isError())
					throw new IllegalArgumentException(r.toString());
			}
		}

		@Override
		public boolean flush(ByteBuffer bbuf) {
			if (c.position() > 0 || !endCalled) {
				CoderResult r = enc.encode(c.flip(), bbuf, true);
				c.compact();
				endCalled = true;
				if (r.isOverflow())
					return true;
				if (r.isError())
					throw new IllegalArgumentException(r.toString());
			}
			CoderResult r = enc.flush(bbuf);
			if (r.isError())
				throw new IllegalArgumentException(r.toString());
			return r.isOverflow();
		}
	}
}
