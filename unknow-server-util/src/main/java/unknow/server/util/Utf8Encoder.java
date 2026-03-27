package unknow.server.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class Utf8Encoder implements Encoder {
	private int surrogate;

	@Override
	public void encode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput) {
		if (!cbuf.hasRemaining() || bbuf.remaining() < 4)
			return;
		if (cbuf.hasArray() && bbuf.hasArray())
			fastEncode(cbuf, bbuf, endOfInput);
		else
			slowEncode(cbuf, bbuf, endOfInput);
	}

	private void fastEncode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput) {
		char[] carr = cbuf.array();
		int cpos = cbuf.position() + cbuf.arrayOffset();
		int clim = cbuf.limit() + cbuf.arrayOffset();

		byte[] barr = bbuf.array();
		int bpos = bbuf.position() + bbuf.arrayOffset();
		int blim = bbuf.limit() - 4 + bbuf.arrayOffset();

		if (surrogate != 0) {
			int low = carr[cpos++];
			int code;
			if (low < 0xDC00 || low > 0xDFFF)
				code = 0xFFFD;
			else
				code = 0x10000 + ((surrogate - 0xD800) << 10) + (low - 0xDC00);
			if (code <= 0x7F)
				barr[bpos++] = (byte) code;
			else if (code <= 0x7FF) {
				barr[bpos++] = (byte) (0xC0 | (code >> 6));
				barr[bpos++] = (byte) (0x80 | (code & 0x3F));
			} else if (code <= 0xFFFF) {
				barr[bpos++] = (byte) (0xE0 | (code >> 12));
				barr[bpos++] = (byte) (0x80 | ((code >> 6) & 0x3F));
				barr[bpos++] = (byte) (0x80 | (code & 0x3F));
			} else {
				barr[bpos++] = (byte) (0xF0 | (code >> 18));
				barr[bpos++] = (byte) (0x80 | ((code >> 12) & 0x3F));
				barr[bpos++] = (byte) (0x80 | ((code >> 6) & 0x3F));
				barr[bpos++] = (byte) (0x80 | (code & 0x3F));
			}
			surrogate = 0;
		}

		loop: while (cpos < clim && bpos < blim) {
			int code = carr[cpos++];
			while (code <= 0x7F) {
				barr[bpos++] = (byte) code;
				if (cpos == clim || bpos == blim)
					break loop;
				code = carr[cpos++];
			}
			if (code >= 0xD800 && code <= 0xDBFF) {
				if (cpos < clim)
					code = 0x10000 + ((code - 0xD800) << 10) + (carr[cpos++] - 0xDC00);
				else if (endOfInput)
					code = 0xFFFD;
				else {
					surrogate = code;
					return;
				}
			} else if (code >= 0xDC00 && code <= 0xDFFF)
				code = 0xFFFD;
			if (code <= 0x7FF) {
				barr[bpos++] = (byte) (0xC0 | (code >> 6));
				barr[bpos++] = (byte) (0x80 | (code & 0x3F));
			} else if (code <= 0xFFFF) {
				barr[bpos++] = (byte) (0xE0 | (code >> 12));
				barr[bpos++] = (byte) (0x80 | ((code >> 6) & 0x3F));
				barr[bpos++] = (byte) (0x80 | (code & 0x3F));
			} else {
				barr[bpos++] = (byte) (0xF0 | (code >> 18));
				barr[bpos++] = (byte) (0x80 | ((code >> 12) & 0x3F));
				barr[bpos++] = (byte) (0x80 | ((code >> 6) & 0x3F));
				barr[bpos++] = (byte) (0x80 | (code & 0x3F));
			}
		}
		cbuf.position(cpos - cbuf.arrayOffset());
		bbuf.position(bpos - bbuf.arrayOffset());
	}

	private void slowEncode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput) {
		if (surrogate != 0) {
			int low = cbuf.get();
			if (low < 0xDC00 || low > 0xDFFF)
				slowAppend(0xFFFD, bbuf);
			else
				slowAppend(0x10000 + ((surrogate - 0xD800) << 10) + (cbuf.get() - 0xDC00), bbuf);
			surrogate = 0;
		}
		while (cbuf.hasRemaining() && bbuf.remaining() >= 4) {
			int code = cbuf.get();
			if (code >= 0xD800 && code <= 0xDBFF) {
				if (cbuf.hasRemaining())
					code = 0x10000 + ((code - 0xD800) << 10) + (cbuf.get() - 0xDC00);
				else if (endOfInput)
					code = 0xFFFD;
				else {
					surrogate = code;
					return;
				}
			} else if (code >= 0xDC00 && code <= 0xDFFF)
				code = 0xFFFD;
			slowAppend(code, bbuf);
		}
	}

	private void slowAppend(int code, ByteBuffer bbuf) {
		if (code <= 0x7F)
			bbuf.put((byte) code);
		else if (code <= 0x7FF) {
			bbuf.put((byte) (0xC0 | (code >> 6)));
			bbuf.put((byte) (0x80 | (code & 0x3F)));
		} else if (code <= 0xFFFF) {
			bbuf.put((byte) (0xE0 | (code >> 12)));
			bbuf.put((byte) (0x80 | ((code >> 6) & 0x3F)));
			bbuf.put((byte) (0x80 | (code & 0x3F)));
		} else {
			bbuf.put((byte) (0xF0 | (code >> 18)));
			bbuf.put((byte) (0x80 | ((code >> 12) & 0x3F)));
			bbuf.put((byte) (0x80 | ((code >> 6) & 0x3F)));
			bbuf.put((byte) (0x80 | (code & 0x3F)));
		}
	}
}
