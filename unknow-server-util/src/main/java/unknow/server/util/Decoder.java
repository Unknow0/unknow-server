package unknow.server.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

public interface Decoder {

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

	public class DefaultDecoder implements Decoder {
		private final CharsetDecoder dec;

		public DefaultDecoder(CharsetDecoder dec) {
			this.dec = dec;
		}

		@Override
		public void decode(ByteBuffer bbuf, CharBuffer cbuf, boolean endOfInput) {
			CoderResult r = dec.decode(bbuf, cbuf, endOfInput);
			if (r.isError())
				throw new IllegalArgumentException(r.toString());
		}
	}
}
