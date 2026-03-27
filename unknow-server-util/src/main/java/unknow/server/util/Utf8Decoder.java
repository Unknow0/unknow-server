package unknow.server.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class Utf8Decoder implements Decoder {
	/** code point in building */
	private int codePoint;
	/** remaining octet to read */
	private int r;

	@Override
	public void decode(ByteBuffer bbuf, CharBuffer cbuf, boolean endOfInput) {
		if (bbuf.hasArray()) {
			byte[] array = bbuf.array();
			int e = bbuf.limit() + bbuf.arrayOffset();
			int i = bbuf.position() + bbuf.arrayOffset();
			while (i < e && cbuf.remaining() > 1)
				append(array[i++] & 0xFF, cbuf);
			bbuf.position(i - bbuf.arrayOffset());
		} else {
			while (bbuf.hasRemaining() && cbuf.remaining() > 1) {
				append(bbuf.get() & 0xFF, cbuf);
			}
		}
		if (endOfInput && !bbuf.hasRemaining() && r > 0)
			throw new IllegalArgumentException("Invalid UTF-8 string, truncated continuation bytes");
	}

	private void append(int b, CharBuffer cbuf) {
		if (r > 0) {
			if ((b >> 6) != 0b10)
				throw new IllegalArgumentException("Invalid UTF-8 continuation byte: " + b);

			codePoint = (codePoint << 6) | (b & 0x3F);
			if (--r == 0)
				append(cbuf);
			return;
		}

		if ((b >> 7) == 0) // 1-byte ASCII
			cbuf.put((char) b);
		else if ((b >> 5) == 0b110) {
			r = 1;
			codePoint = b & 0x1F;
		} else if ((b >> 4) == 0b1110) {
			r = 2;
			codePoint = b & 0x0F;
		} else if ((b >> 3) == 0b11110) {
			r = 3;
			codePoint = b & 0x07;
		} else
			throw new IllegalArgumentException("Invalid UTF-8 start byte: " + b);
	}

	private void append(CharBuffer cbuf) {
		if (codePoint > 0xFFFF) { // Surrogate pair
			int cp = codePoint - 0x10000;
			cbuf.put((char) ((cp >> 10) + 0xD800)).put((char) ((cp & 0x3FF) + 0xDC00));
		} else
			cbuf.put((char) codePoint);
		codePoint = 0;
	}
}
