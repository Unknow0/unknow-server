package unknow.server.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

public class Utf8Decoder implements Decoder {
	private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
	private static final VarHandle SHORT = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

	private final CharsetDecoder ascii = StandardCharsets.US_ASCII.newDecoder();

	/** code point in building */
	private int cp;
	/** min code point allowed */
	private int minCp;
	/** remaining octet to read */
	private int r;

	@Override
	public void decode(ByteBuffer bbuf, CharBuffer cbuf, boolean endOfInput) {
		if (bbuf.hasArray() && cbuf.hasArray())
			fastDecode(bbuf, cbuf);
		else {
			slowDecode(bbuf.order(ByteOrder.LITTLE_ENDIAN), cbuf);
		}
		if (endOfInput && !bbuf.hasRemaining() && r > 0)
			throw new IllegalArgumentException("Invalid UTF-8 string, truncated continuation bytes");
	}

	@Override
	public boolean flush(CharBuffer cbuf) {
		return false;
	}

	private void fastDecode(ByteBuffer bbuf, CharBuffer cbuf) {

		byte[] barr = bbuf.array();
		int blim = bbuf.limit() + bbuf.arrayOffset();
		char[] carr = cbuf.array();
		int clim = cbuf.limit() - 1 + cbuf.arrayOffset();
		if (r > 0) {
			int bpos = bbuf.position() + bbuf.arrayOffset();
			int cpos = cbuf.position() + cbuf.arrayOffset();
			remainingArray(bbuf, bpos, cbuf, cpos, cp, r);
			if (r > 0)
				return;
		}
		ascii.decode(bbuf, cbuf, false);
		int bpos = bbuf.position() + bbuf.arrayOffset();
		int cpos = cbuf.position() + cbuf.arrayOffset();
		int code;
		while (bpos < blim && cpos < clim) {
			int b = barr[bpos++];
			if (b >= 0)
				carr[cpos++] = (char) b;
			else if (b < -64)
				error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 start byte: 0x" + Integer.toString(b & 0xFF, 16));
			else if (b < -32) {
				code = b & 0x1F;
				if (bpos == blim) {
					this.cp = code;
					r = 1;
					minCp = 0x80;
					updatePos(bbuf, bpos, cbuf, cpos);
					return;
				}
				b = barr[bpos++];
				if ((b & 0xc0) != 0x80)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 continuation byte");
				code = (code << 6) | (b & 0x3F);
				if (code < 0x80)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 overlong sequence");
				carr[cpos++] = (char) code;
			} else if (b < -16) {
				code = b & 0x0F;
				if (bpos + 1 >= blim) {
					minCp = 0x800;
					remainingArray(bbuf, bpos, cbuf, cpos, code, 2);
					return;
				}
				short s = (short) SHORT.get(barr, bpos);
				if ((s & 0xc0c0) != 0x8080)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 continuation byte");

				code = (code << 12) | ((s & 0x3F) << 6) | ((s >> 8) & 0x3F);
				if (code < 0x800)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 overlong sequence");
				carr[cpos++] = (char) code;
				bpos += 2;
			} else if (b < -8) {
				code = b & 0x07;
				if (bpos + 3 >= blim) {
					minCp = 0x10000;
					remainingArray(bbuf, bpos, cbuf, cpos, code, 3);
					return;
				}
				int i = (int) INT.get(barr, bpos);
				if ((i & 0x00C0C0C0) != 0x00808080)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 continuation byte");
				code = (code << 18) | ((i & 0x3F) << 12) | (((i >> 8) & 0x3F) << 6) | ((i >> 16) & 0x3F);
				if (code < 0x10000)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 overlong sequence");
				if (code > 0xFFFF) { // Surrogate pair
					code -= 0x10000;
					carr[cpos++] = (char) ((code >> 10) | 0xD800);
					carr[cpos++] = (char) ((code & 0x3FF) | 0xDC00);
				} else
					carr[cpos++] = (char) code;
				bpos += 3;
			} else
				error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 start byte: 0x" + Integer.toString(b, 16));
		}
		updatePos(bbuf, bpos, cbuf, cpos);
	}

	private void remainingArray(ByteBuffer bbuf, int bpos, CharBuffer cbuf, int cpos, int cp, int r) {
		byte[] barr = bbuf.array();
		int blim = bbuf.limit() + bbuf.arrayOffset();
		char[] carr = cbuf.array();
		int clim = cbuf.limit() - 1 + cbuf.arrayOffset();

		while (bpos < blim && cpos < clim) {
			int b = barr[bpos++];
			if ((b & 0xc0) != 0x80)
				error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 continuation byte");
			cp = (cp << 6) | (b & 0x3F);
			if (--r == 0) {
				if (cp < minCp)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 overlong sequence " + Integer.toString(cp, 16) + " < " + Integer.toString(minCp, 16));
				if (cp > 0xFFFF) { // Surrogate pair
					cp -= 0x10000;
					carr[cpos++] = (char) ((cp >> 10) | 0xD800);
					carr[cpos++] = (char) ((cp & 0x3FF) | 0xDC00);
				} else
					carr[cpos++] = (char) cp;
				this.r = r;
				updatePos(bbuf, bpos, cbuf, cpos);
				return;
			}
		}
		this.cp = cp;
		this.r = r;
		updatePos(bbuf, bpos, cbuf, cpos);
	}

	private static void error(ByteBuffer bbuf, int bpos, CharBuffer cbuf, int cpos, String msg) {
		updatePos(bbuf, bpos, cbuf, cpos);
		throw new IllegalArgumentException(msg);
	}

	private static void updatePos(ByteBuffer bbuf, int bpos, CharBuffer cbuf, int cpos) {
		bbuf.position(bpos - bbuf.arrayOffset());
		cbuf.position(cpos - cbuf.arrayOffset());
	}

	private void slowDecode(ByteBuffer bbuf, CharBuffer cbuf) {
		if (r > 0 && slowRemaining(bbuf, cbuf, cp, r))
			return;

		int code;
		while (bbuf.hasRemaining() && cbuf.remaining() > 1) {
			int b = bbuf.get();
			if (b >= 0)
				cbuf.put((char) b);
			else if (b < -64)
				throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b & 0xFF, 16));
			else if (b < -32) {
				code = b & 0x1F;
				if (!bbuf.hasRemaining()) {
					minCp = 0x80;
					r = 1;
					this.cp = code;
					return;
				}
				b = bbuf.get();
				if ((b & 0xc0) != 0x80)
					throw new IllegalArgumentException("Invalid UTF-8 continuation byte");
				code = (code << 6) | (b & 0x3F);
				if (code < 0x80)
					throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
				cbuf.put((char) code);
			} else if (b < -16) {
				code = b & 0x0F;
				if (bbuf.remaining() < 2) {
					minCp = 0x800;
					slowRemaining(bbuf, cbuf, code, 2);
					return;
				}
				short s = bbuf.getShort();
				if ((s & 0xc0c0) != 0x8080)
					throw new IllegalArgumentException("Invalid UTF-8 continuation byte");

				code = (code << 12) | ((s & 0x3F) << 6) | ((s >> 8) & 0x3F);
				if (code < 0x800)
					throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
				cbuf.put((char) code);
			} else if (b < -8) {
				code = b & 0x07;
				if (bbuf.remaining() < 4) {
					minCp = 0x10000;
					slowRemaining(bbuf, cbuf, code, 3);
					return;
				}
				int i = bbuf.getInt(bbuf.position());
				bbuf.position(bbuf.position() + 3);
				if ((i & 0x00C0C0C0) != 0x00808080)
					throw new IllegalArgumentException("Invalid UTF-8 continuation byte");
				code = (code << 18) | ((i & 0x3F) << 12) | (((i >> 8) & 0x3F) << 6) | ((i >> 16) & 0x3F);
				if (code < 0x10000)
					throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
				slowAppend(cbuf, code);
			} else
				throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b, 16));
		}
	}

	private boolean slowRemaining(ByteBuffer bbuf, CharBuffer cbuf, int cp, int r) {
		while (bbuf.hasRemaining() && cbuf.remaining() > 1) {
			int b = bbuf.get();
			if ((b & 0xc0) != 0x80)
				throw new IllegalArgumentException("Invalid UTF-8 continuation byte");
			cp = (cp << 6) | (b & 0x3F);
			if (--r == 0)
				slowAppend(cbuf, cp);
		}
		this.cp = cp;
		this.r = r;
		return r > 0;
	}

	private void slowAppend(CharBuffer cbuf, int cp) {
		if (cp < minCp)
			throw new IllegalArgumentException("Invalid UTF-8 overlong sequence " + Integer.toString(cp, 16) + " < " + Integer.toString(minCp, 16));
		if (cp > 0xFFFF) { // Surrogate pair
			cp -= 0x10000;
			cbuf.put((char) ((cp >> 10) + 0xD800)).put((char) ((cp & 0x3FF) + 0xDC00));
		} else
			cbuf.put((char) cp);
	}
}
