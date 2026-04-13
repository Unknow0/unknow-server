package unknow.server.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

public interface Decoder {
	/**
	 * create a nex decoder for a charset
	 * @param charset the charset
	 * @return the decoder
	 */
	public static Decoder from(Charset charset) {
		if (charset.equals(StandardCharsets.UTF_8))
			return new Utf8Decoder();
		return new DefaultDecoder(charset.newDecoder());
	}

	/**
	 * append one byte
	 * @param bbuf byte to append
	 * @param cbuf produced data
	 * @param endOfInput the data is done
	 */
	void decode(ByteBuffer bbuf, CharBuffer cbuf, boolean endOfInput);

	/**
	 * flush remaining char
	 * @param cbuf output
	 * @return true if we need to recall flush with more space
	 */
	boolean flush(CharBuffer cbuf);

	public class DefaultDecoder implements Decoder {
		private final CharsetDecoder dec;
		private final ByteBuffer b;
		private boolean endCalled;

		public DefaultDecoder(CharsetDecoder dec) {
			this.dec = dec;
			this.b = ByteBuffer.allocate(4096);
			this.endCalled = false;
		}

		@Override
		public void decode(ByteBuffer bbuf, CharBuffer cbuf, boolean endOfInput) {
			int l = bbuf.limit();
			while (bbuf.position() < l) {
				b.put(bbuf.limit(Math.min(l, bbuf.position() + b.remaining())));
				CoderResult r = dec.decode(b.flip(), cbuf, endOfInput);
				b.compact();
				bbuf.limit(l);
				if (endOfInput)
					endCalled = true;
				if (r.isOverflow())
					return;
				if (r.isError())
					throw new IllegalArgumentException(r.toString());
			}
		}

		@Override
		public boolean flush(CharBuffer cbuf) {
			if (b.position() > 0 || !endCalled) {
				CoderResult r = dec.decode(b.flip(), cbuf, true);
				b.compact();
				endCalled = true;
				if (r.isOverflow())
					return true;
				if (r.isError())
					throw new IllegalArgumentException(r.toString());
			}
			CoderResult r = dec.flush(cbuf);
			if (r.isError())
				throw new IllegalArgumentException(r.toString());
			return r.isOverflow();
		}
	}
}
