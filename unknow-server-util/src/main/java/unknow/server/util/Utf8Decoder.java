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

		int codePoint = this.codePoint;
		int r = this.r;

		if (r > 0) {
			if (bpos + r <= blim) { // can read all
				int b;
				switch (r) {
					case 3:
						b = barr[bpos++] & 0xFF;
						if ((b >> 6) != 0b10)
							throw new IllegalArgumentException("Invalid UTF-8 continuation byte: " + b);
						codePoint = (codePoint << 6) | (b & 0x3F);
						if (codePoint < 0x10)
							throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
					case 2:
						b = barr[bpos++] & 0xFF;
						if ((b >> 6) != 0b10)
							throw new IllegalArgumentException("Invalid UTF-8 continuation byte: " + b);
						codePoint = (codePoint << 6) | (b & 0x3F);
						if (codePoint < 0x20)
							throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
					case 1:
						b = barr[bpos++] & 0xFF;
						if ((b >> 6) != 0b10)
							throw new IllegalArgumentException("Invalid UTF-8 continuation byte: " + b);
						codePoint = (codePoint << 6) | (b & 0x3F);
				}
				if (codePoint > 0xFFFF) { // Surrogate pair
					int cp = codePoint - 0x10000;
					carr[cpos++] = (char) ((cp >> 10) + 0xD800);
					carr[cpos++] = (char) ((cp & 0x3FF) + 0xDC00);
				} else
					carr[cpos++] = (char) codePoint;
				r = 0;
			} else {
				while (bpos < blim && cpos < clim) {
					int b = barr[bpos++] & 0xFF;
					if ((b >> 6) != 0b10)
						throw new IllegalArgumentException("Invalid UTF-8 continuation byte: " + b);
					codePoint = (codePoint << 6) | (b & 0x3F);
					if ((r == 2 && codePoint < 0x20) || (r == 3 && codePoint < 0x10))
						throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
					if (--r == 0) {
						if (codePoint > 0xFFFF) { // Surrogate pair
							int cp = codePoint - 0x10000;
							carr[cpos++] = (char) ((cp >> 10) + 0xD800);
							carr[cpos++] = (char) ((cp & 0x3FF) + 0xDC00);
						} else
							carr[cpos++] = (char) codePoint;
					}
				}
			}
		}

		while (bpos < blim && cpos < clim) {
			int b = barr[bpos++] & 0xFF;
			if (b < 0x80) { // 1-byte ASCII
				carr[cpos++] = (char) b;
				int maxAscii = Math.min(clim, cpos + blim - bpos);
				while (cpos < maxAscii && barr[bpos] >= 0)
					carr[cpos++] = (char) barr[bpos++];
			} else if (b < 0xc0)
				throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b, 16));
			else {
				if (b < 0xe0) {
					codePoint = b & 0x1F;
					if (codePoint < 2)
						throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
					if (bpos < blim) {
						b = barr[bpos++] & 0xFF;
						if ((b >> 6) != 0b10)
							throw new IllegalArgumentException("Invalid UTF-8 continuation byte: 0x" + Integer.toString(b, 16));
						codePoint = (codePoint << 6) | (b & 0x3F);
						if (codePoint > 0xFFFF) { // Surrogate pair
							int cp = codePoint - 0x10000;
							carr[cpos++] = (char) ((cp >> 10) + 0xD800);
							carr[cpos++] = (char) ((cp & 0x3FF) + 0xDC00);
						} else
							carr[cpos++] = (char) codePoint;
					} else {
						r = 1;
						break;
					}
				} else if (b < 0xf0) {
					codePoint = b & 0x0F;
					if (bpos + 1 < blim) {
						b = barr[bpos++] & 0xFF;
						if ((b >> 6) != 0b10)
							throw new IllegalArgumentException("Invalid UTF-8 continuation byte: 0x" + Integer.toString(b, 16));
						codePoint = (codePoint << 6) | (b & 0x3F);
						if (codePoint < 0x20)
							throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
						b = barr[bpos++] & 0xFF;
						if ((b >> 6) != 0b10)
							throw new IllegalArgumentException("Invalid UTF-8 continuation byte: 0x" + Integer.toString(b, 16));
						codePoint = (codePoint << 6) | (b & 0x3F);
						if (codePoint > 0xFFFF) { // Surrogate pair
							int cp = codePoint - 0x10000;
							carr[cpos++] = (char) ((cp >> 10) + 0xD800);
							carr[cpos++] = (char) ((cp & 0x3FF) + 0xDC00);
						} else
							carr[cpos++] = (char) codePoint;
					} else {
						r = 2;
						break;
					}
				} else if (b < 0xf4) {
					codePoint = b & 0x07;
					if (bpos + 2 < blim) {
						b = barr[bpos++] & 0xFF;
						if ((b >> 6) != 0b10)
							throw new IllegalArgumentException("Invalid UTF-8 continuation byte: 0x" + Integer.toString(b, 16));
						codePoint = (codePoint << 6) | (b & 0x3F);
						if (codePoint < 0x10)
							throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");

						b = barr[bpos++] & 0xFF;
						if ((b >> 6) != 0b10)
							throw new IllegalArgumentException("Invalid UTF-8 continuation byte: 0x" + Integer.toString(b, 16));
						codePoint = (codePoint << 6) | (b & 0x3F);
						b = barr[bpos++] & 0xFF;
						if ((b >> 6) != 0b10)
							throw new IllegalArgumentException("Invalid UTF-8 continuation byte: 0x" + Integer.toString(b, 16));
						codePoint = (codePoint << 6) | (b & 0x3F);
						if (codePoint > 0xFFFF) { // Surrogate pair
							int cp = codePoint - 0x10000;
							carr[cpos++] = (char) ((cp >> 10) + 0xD800);
							carr[cpos++] = (char) ((cp & 0x3FF) + 0xDC00);
						} else
							carr[cpos++] = (char) codePoint;
					} else {
						r = 3;
						break;
					}
				} else
					throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b, 16));
			}
		}

		if (r > 0) {
			while (bpos < blim && cpos < clim) {
				int b = barr[bpos++] & 0xFF;
				if ((b >> 6) != 0b10)
					throw new IllegalArgumentException("Invalid UTF-8 continuation byte: 0x" + Integer.toString(b, 16));
				codePoint = (codePoint << 6) | (b & 0x3F);
				if (--r == 0) {
					if (codePoint > 0xFFFF) { // Surrogate pair
						int cp = codePoint - 0x10000;
						carr[cpos++] = (char) ((cp >> 10) + 0xD800);
						carr[cpos++] = (char) ((cp & 0x3FF) + 0xDC00);
					} else
						carr[cpos++] = (char) codePoint;
				}
			}
		}

		this.r = r;
		this.codePoint = codePoint;

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
				throw new IllegalArgumentException("Invalid UTF-8 continuation byte: 0x" + Integer.toString(b, 16));

			codePoint = (codePoint << 6) | (b & 0x3F);
			if ((r == 2 && codePoint < 0x20) || (r == 3 && codePoint < 0x10))
				throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
			if (--r == 0)
				slowAppend(cbuf);
		} else if (b < 0x80) // 1-byte ASCII
			cbuf.put((char) b);
		else if (b < 0xc0)
			throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b, 16));
		else if (b < 0xe0) {
			r = 1;
			codePoint = b & 0x1F;
			if (codePoint < 2)
				throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
		} else if (b < 0xf0) {
			r = 2;
			codePoint = b & 0x0F;
		} else if (b < 0xfb) {
			r = 3;
			codePoint = b & 0x07;
		} else
			throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b, 16));
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
