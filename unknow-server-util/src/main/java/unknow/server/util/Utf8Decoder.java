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
		if (bbuf.hasArray() && cbuf.hasArray())
			fastDecode(bbuf, cbuf);
		else {
			slowDecode(bbuf, cbuf);
		}
		if (endOfInput && !bbuf.hasRemaining() && r > 0)
			throw new IllegalArgumentException("Invalid UTF-8 string, truncated continuation bytes");
	}

	private void fastDecode(ByteBuffer bbuf, CharBuffer cbuf) {
		byte[] barr = bbuf.array();
		int bpos = bbuf.position() + bbuf.arrayOffset();
		int blim = bbuf.limit() + bbuf.arrayOffset();

		char[] carr = cbuf.array();
		int cpos = cbuf.position() + cbuf.arrayOffset();
		int clim = cbuf.limit() - 1 + cbuf.arrayOffset();
		while (bpos < blim && cpos < clim) {
			int b = barr[bpos++] & 0xFF;
			if (r > 0) {
				if ((b >> 6) != 0b10)
					throw new IllegalArgumentException("Invalid UTF-8 continuation byte: " + b);

				codePoint = (codePoint << 6) | (b & 0x3F);
				if (--r == 0) {
					if (codePoint > 0xFFFF) { // Surrogate pair
						int cp = codePoint - 0x10000;
						carr[cpos++] = (char) ((cp >> 10) + 0xD800);
						carr[cpos++] = (char) ((cp & 0x3FF) + 0xDC00);
					} else
						carr[cpos++] = (char) codePoint;
					codePoint = 0;
				}
			} else if (b < 0x80) { // 1-byte ASCII
				carr[cpos++] = (char) b;
				while (bpos < blim && cpos < clim) {
					b = barr[bpos];
					if (b < 0)
						break;
					carr[cpos++] = (char) b;
					bpos++;
				}
			} else if (b < 0xc0)
				throw new IllegalArgumentException("Invalid UTF-8 start byte: " + b);
			else if (b < 0xe0) {
				r = 1;
				codePoint = b & 0x1F;
			} else if (b < 0xf0) {
				r = 2;
				codePoint = b & 0x0F;
			} else if (b < 0xfb) {
				r = 3;
				codePoint = b & 0x07;
			} else
				throw new IllegalArgumentException("Invalid UTF-8 start byte: " + b);
		}
		bbuf.position(bpos - bbuf.arrayOffset());
		cbuf.position(cpos - cbuf.arrayOffset());
	}

	private void slowDecode(ByteBuffer bbuf, CharBuffer cbuf) {
		while (bbuf.hasRemaining() && cbuf.remaining() > 1) {
			slowAppend(bbuf.get() & 0xFF, cbuf);
		}
	}

	private void slowAppend(int b, CharBuffer cbuf) {
		if (r > 0) {
			if ((b >> 6) != 0b10)
				throw new IllegalArgumentException("Invalid UTF-8 continuation byte: " + b);

			codePoint = (codePoint << 6) | (b & 0x3F);
			if (--r == 0)
				slowAppend(cbuf);
		} else if (b < 0x80) // 1-byte ASCII
			cbuf.put((char) b);
		else if (b < 0xc0)
			throw new IllegalArgumentException("Invalid UTF-8 start byte: " + b);
		else if (b < 0xe0) {
			r = 1;
			codePoint = b & 0x1F;
		} else if (b < 0xf0) {
			r = 2;
			codePoint = b & 0x0F;
		} else if (b < 0xfb) {
			r = 3;
			codePoint = b & 0x07;
		} else
			throw new IllegalArgumentException("Invalid UTF-8 start byte: " + b);
	}

	private void slowAppend(CharBuffer cbuf) {
		if (codePoint > 0xFFFF) { // Surrogate pair
			int cp = codePoint - 0x10000;
			cbuf.put((char) ((cp >> 10) + 0xD800)).put((char) ((cp & 0x3FF) + 0xDC00));
		} else
			cbuf.put((char) codePoint);
		codePoint = 0;
	}
}
