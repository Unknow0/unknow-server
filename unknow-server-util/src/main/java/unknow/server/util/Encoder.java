package unknow.server.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

public interface Encoder {

	public static Encoder from(Charset charset) {
		if (charset.equals(StandardCharsets.UTF_8))
			return new Utf8Encoder();
		return new DefaultEncoder(charset.newEncoder());
	}

	/**
	 * append one byte
	 * @param cbuf char to encode
	 * @param bbuf byte output
	 * @param endOfInput the data is done
	 */
	void encode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput);

	public class DefaultEncoder implements Encoder {
		private final CharsetEncoder enc;

		public DefaultEncoder(CharsetEncoder dec) {
			this.enc = dec;
		}

		@Override
		public void encode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput) {
			CoderResult r = enc.encode(cbuf, bbuf, endOfInput);
			if (r.isError())
				throw new IllegalArgumentException(r.toString());
		}
	}
}
