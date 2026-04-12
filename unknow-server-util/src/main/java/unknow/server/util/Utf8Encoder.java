package unknow.server.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

public class Utf8Encoder implements Encoder {
	private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);

	private static final int repl = 0x00BDBFEF;

	private final CharsetEncoder ascii = StandardCharsets.US_ASCII.newEncoder();

	private int hi;

	@Override
	public void encode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput) {
		if (!cbuf.hasRemaining() || bbuf.remaining() < 4)
			return;
		if (cbuf.hasArray() && bbuf.hasArray())
			fastEncode(cbuf, bbuf, endOfInput);
		else
			slowEncode(cbuf, bbuf.order(ByteOrder.LITTLE_ENDIAN), endOfInput);
	}

	@Override
	public boolean flush(ByteBuffer bbuf) {
		if (hi != 0) {
			if (bbuf.remaining() < 3)
				return true;
			bbuf.put(REPL);
			hi = 0;
		}
		return false;
	}

	private void fastEncode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput) {
		char[] carr = cbuf.array();
		int coff = cbuf.arrayOffset();
		int clim = cbuf.limit() + coff;

		byte[] barr = bbuf.array();
		int boff = bbuf.arrayOffset();
		int blim = bbuf.limit() - 3 + boff;

		if (hi != 0) {
			int bpos = bbuf.position() + boff;
			int cpos = cbuf.position() + coff;
			int low = carr[cpos];
			if (low >= 0xDC00 && low <= 0xDFFF) {
				cbuf.position(cpos - coff + 1);
				int c = 0x10000 + ((hi & 0x7FF) << 10) + (low & 0x3FF);
				c = 0x808080F0 | (c >> 18) | ((c >> 4) & 0x00003F00) | ((c << 10) & 0x003F0000) | (c << 24 & 0x3F000000);
				INT.set(barr, bpos, c);
				bbuf.position(bpos - boff + 4);
			} else {
				INT.set(barr, bpos, repl);
				bbuf.position(bpos - boff + 3);
			}
			hi = 0;
		}

		ascii.encode(cbuf, bbuf, false);
		int bpos = bbuf.position() + boff;
		int cpos = cbuf.position() + coff;

		while (cpos < clim && bpos < blim) {
			char code = carr[cpos++];
			if (code < 0x80) {
				barr[bpos++] = (byte) code;
			} else if (code < 0x800) {
				int c = 0x80C0 | (code >> 6) | ((code << 8) & 0x3F00);
				INT.set(barr, bpos, c);
				bpos += 2;
			} else if (code < 0xD800) {
				int c = 0x008080E0 | (code >> 12) | ((code << 2) & 0x3F00) | (code << 16 & 0x3F0000);
				INT.set(barr, bpos, c);
				bpos += 3;
			} else if (code <= 0xDC00) {
				// high surrogate
				if (cpos < clim) {
					int low = carr[cpos];
					if (low >= 0xDC00 && low <= 0xDFFF) {
						cpos++;
						int c = 0x10000 + ((code & 0x7FF) << 10) + (low & 0x3FF);
						c = 0x808080F0 | (c >> 18) | ((c >> 4) & 0x00003F00) | ((c << 10) & 0x003F0000) | (c << 24 & 0x3F000000);
						INT.set(barr, bpos, c);
						bpos += 4;
					} else {
						INT.set(barr, bpos, repl);
						bpos += 3;
					}
				} else if (endOfInput) {
					INT.set(barr, bpos, repl);
					bpos += 3;
				} else {
					hi = code;
					break;
				}
			} else if (code < 0xE000) { // lone low surrogate
				INT.set(barr, bpos, repl);
				bpos += 3;
			} else {
				int c = 0x008080E0 | (code >> 12) | ((code << 2) & 0x3F00) | (code << 16 & 0x3F0000);
				INT.set(barr, bpos, c);
				bpos += 3;
			}
		}
		cbuf.position(cpos - coff);
		bbuf.position(bpos - boff);
	}

	private void slowEncode(CharBuffer cbuf, ByteBuffer bbuf, boolean endOfInput) {
		if (hi != 0) {
			int cpos = cbuf.position();
			int low = cbuf.get(cpos);
			if (slowSurrogate(bbuf, hi, low))
				cbuf.position(cpos + 1);
			hi = 0;
		}
		while (cbuf.hasRemaining() && bbuf.remaining() >= 4) {
			int code = cbuf.get();
			if (code < 0x80) {
				bbuf.put((byte) code);
			} else if (code < 0x800) {
				bbuf.putShort((short) (0x80C0 | (code >> 6) | (code & 0x3F)));
			} else if (code < 0xD800) {
				int p = bbuf.position();
				int c = 0x008080E0 | (code >> 12) | ((code << 2) & 0x3F00) | (code << 16 & 0x3F0000);
				bbuf.putInt(p, c).position(p + 3);
			} else if (code <= 0xDC00) {
				// high surrogate
				if (cbuf.hasRemaining()) {
					int cpos = cbuf.position();
					int low = cbuf.get(cpos);
					if (slowSurrogate(bbuf, code, low))
						cbuf.position(cpos + 1);
				} else if (endOfInput)
					bbuf.put(REPL);
				else {
					hi = code;
					break;
				}
			} else if (code < 0xE000)  // lone low surrogate
				bbuf.put(REPL);
			else {
				int p = bbuf.position();
				int c = 0x008080E0 | (code >> 12) | ((code << 2) & 0x3F00) | (code << 16 & 0x3F0000);
				bbuf.putInt(p, c).position(p + 3);
			}
		}
	}

	private boolean slowSurrogate(ByteBuffer bbuf, int high, int low) {
		if (low < 0xDC00 && low > 0xDFFF) {
			bbuf.put(REPL);
			return false;
		}
		int c = 0x10000 + ((high & 0x7FF) << 10) + (low & 0x3FF);
		c = 0x808080F0 | (c >> 18) | ((c >> 4) & 0x00003F00) | ((c << 10) & 0x003F0000) | (c << 24 & 0x3F000000);
		bbuf.putInt(c);
		return true;
	}
}
