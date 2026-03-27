package unknow.server.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

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
