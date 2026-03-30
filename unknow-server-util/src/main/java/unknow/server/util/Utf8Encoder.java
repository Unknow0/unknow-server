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
		int blim = bbuf.limit() - 3 + bbuf.arrayOffset();

		if (surrogate != 0) {
			int low = carr[cpos];
			if (low >= 0xDC00 && low <= 0xDFFF) {
				cpos++;
				int code = 0x10000 + ((surrogate - 0xD800) << 10) + (low - 0xDC00);
				barr[bpos++] = (byte) (0xF0 | (code >> 18));
				barr[bpos++] = (byte) (0x80 | ((code >> 12) & 0x3F));
				barr[bpos++] = (byte) (0x80 | ((code >> 6) & 0x3F));
				barr[bpos++] = (byte) (0x80 | (code & 0x3F));
			} else {
				barr[bpos++] = (byte) 0xEF;
				barr[bpos++] = (byte) 0xBF;
				barr[bpos++] = (byte) 0xbd;
			}
			surrogate = 0;
		}
		int maxAscii = Math.min(clim, cpos + blim - bpos);
		while (cpos < maxAscii && carr[cpos] <= 0x7f)
			barr[bpos++] = (byte) carr[cpos++];
		while (cpos < clim && bpos < blim) {
			int code = carr[cpos++];
			if (code <= 0x7f) {
				barr[bpos++] = (byte) code;
				while (cpos < maxAscii && carr[cpos] <= 0x7f)
					barr[bpos++] = (byte) carr[cpos++];
			} else if (code <= 0x7FF) {
				barr[bpos++] = (byte) (0xC0 | (code >> 6));
				barr[bpos++] = (byte) (0x80 | (code & 0x3F));
			} else {
				int i = code >> 10;
				if (i == 0b110110) { // high surrogate
					if (cpos < clim) {
						int low = carr[cpos];
						if (low >= 0xDC00 && low <= 0xDFFF) {
							cpos++;
							int c = 0x10000 + ((code - 0xD800) << 10) + (low - 0xDC00);
							barr[bpos++] = (byte) (0xF0 | (c >> 18));
							barr[bpos++] = (byte) (0x80 | ((c >> 12) & 0x3F));
							barr[bpos++] = (byte) (0x80 | ((c >> 6) & 0x3F));
							barr[bpos++] = (byte) (0x80 | (c & 0x3F));
						} else {
							barr[bpos++] = (byte) 0xEF;
							barr[bpos++] = (byte) 0xBF;
							barr[bpos++] = (byte) 0xbd;
						}
					} else if (endOfInput) {
						barr[bpos++] = (byte) 0xEF;
						barr[bpos++] = (byte) 0xBF;
						barr[bpos++] = (byte) 0xbd;
					} else {
						surrogate = code;
						break;
					}
				} else if (i == 0b110111) { // lone low surrogate
					barr[bpos++] = (byte) 0xEF;
					barr[bpos++] = (byte) 0xBF;
					barr[bpos++] = (byte) 0xbd;
				} else {
					barr[bpos++] = (byte) (0xE0 | (code >> 12));
					barr[bpos++] = (byte) (0x80 | ((code >> 6) & 0x3F));
					barr[bpos++] = (byte) (0x80 | (code & 0x3F));
				}
			}
		}
		cbuf.position(cpos - cbuf.arrayOffset());
		bbuf.position(bpos - bbuf.arrayOffset());
	}

	private void slowEncode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput) {
		if (surrogate != 0) {
			int low = cbuf.get(cbuf.position());
			if (low >= 0xDC00 && low <= 0xDFFF) {
				slowAppend(0x10000 + ((surrogate - 0xD800) << 10) + (low - 0xDC00), bbuf);
				cbuf.position(cbuf.position() + 1);
			} else
				slowAppend(0xFFFD, bbuf);
			surrogate = 0;
		}
		int cpos = bbuf.position();
		int maxAscii = Math.min(cbuf.limit(), cpos + bbuf.remaining());
		while (cpos < maxAscii && cbuf.get(cpos++) <= 0x7f)
			bbuf.put((byte) cbuf.get());
		while (cbuf.hasRemaining() && bbuf.remaining() >= 4) {
			int code = cbuf.get();
			int i = code >> 10;
			if (i == 0b110110) { // high surrogate
				if (cbuf.hasRemaining()) {
					int low = cbuf.get(cbuf.position());
					if (low >= 0xDC00 && low <= 0xDFFF) {
						code = 0x10000 + ((code - 0xD800) << 10) + (low - 0xDC00);
						cbuf.position(cbuf.position() + 1);
					} else
						code = 0xFFFD;
				} else if (endOfInput)
					code = 0xFFFD;
				else {
					surrogate = code;
					return;
				}
			} else if (i == 0b110111) // lone low surrogate
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
