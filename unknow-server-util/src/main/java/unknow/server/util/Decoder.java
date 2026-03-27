package unknow.server.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
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
}
