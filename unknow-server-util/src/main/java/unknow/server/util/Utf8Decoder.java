package unknow.server.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;

public class Utf8Decoder implements Decoder {
	private static final VarHandle LONG = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
	private static final VarHandle INT = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
	private static final VarHandle SHORT = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

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
		if (r > 0) {
			remainingArray(bbuf, bpos, cbuf, cpos, cp, r);
			if (r > 0)
				return;
			bpos = bbuf.position() + bbuf.arrayOffset();
			cpos = cbuf.position() + cbuf.arrayOffset();
		}
		int max = Math.min(clim, cpos + blim - bpos) - 8;
		while (cpos < max) {
			long l = (long) LONG.get(barr, bpos);
			if ((l & 0x8080808080808080L) != 0L)
				break;
			carr[cpos++] = (char) l;
			carr[cpos++] = (char) (l >>> 8);
			carr[cpos++] = (char) (l >>> 16);
			carr[cpos++] = (char) (l >>> 24);
			carr[cpos++] = (char) (l >>> 32);
			carr[cpos++] = (char) (l >>> 40);
			carr[cpos++] = (char) (l >>> 48);
			carr[cpos++] = (char) (l >>> 56);
			bpos += 8;
		}
		int cp;
		loop: while (bpos < blim && cpos < clim) {
			int b = barr[bpos++];

			if (b >= 0)
				carr[cpos++] = (char) b;
			else if (b < -64)
				throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b & 0xFF, 16));
			else if (b < -32) {
				cp = b & 0x1F;
				if (bpos == blim) {
					this.cp = cp;
					r = 1;
					minCp = 0x80;
					break loop;
				}
				cp = continuation(cp, barr[bpos++]);
				if (cp < 0x80)
					throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
				carr[cpos++] = (char) cp;
			} else if (b < -16) {
				cp = b & 0x0F;
				if (bpos + 1 >= blim) {
					minCp = 0x800;
					remainingArray(bbuf, bpos, cbuf, cpos, cp, 2);
					return;
				}
				short s = (short) SHORT.get(barr, bpos);
				if ((s & 0xc0c0) != 0x8080)
					throw new IllegalArgumentException("Invalid UTF-8 continuation byte");

				cp = (cp << 12) ^ ((s & 0x3F) << 6) ^ ((s >> 8) & 0x3F);
				if (cp < 0x800)
					throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
				carr[cpos++] = (char) cp;
				bpos += 2;
			} else if (b < -8) {
				cp = b & 0x07;
				if (bpos + 3 >= blim) {
					minCp = 0x10000;
					remainingArray(bbuf, bpos, cbuf, cpos, cp, 3);
					return;
				}
				int i = (int) INT.get(barr, bpos);
				if ((i & 0x00C0C0C0) != 0x00808080)
					throw new IllegalArgumentException("Invalid UTF-8 continuation byte");
				cp = (cp << 18) ^ ((i & 0x3F) << 12) ^ (((i >> 8) & 0x3F) << 6) ^ (((i >> 16) & 0x3F));
				if (cp < 0x10000)
					throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
				if (cp > 0xFFFF) { // Surrogate pair
					cp -= 0x10000;
					carr[cpos++] = (char) ((cp >> 10) ^ 0xD800);
					carr[cpos++] = (char) ((cp & 0x3FF) ^ 0xDC00);
				} else
					carr[cpos++] = (char) cp;
				bpos += 3;
			} else
				throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b, 16));
		}
		updatePos(bbuf, bpos, cbuf, cpos);
	}

	private void remainingArray(ByteBuffer bbuf, int bpos, CharBuffer cbuf, int cpos, int cp, int r) {
		byte[] barr = bbuf.array();
		int blim = bbuf.limit() + bbuf.arrayOffset();
		char[] carr = cbuf.array();
		int clim = cbuf.limit() - 1 + cbuf.arrayOffset();

		while (bpos < blim && cpos < clim) {
			cp = continuation(cp, barr[bpos++]);
			if (--r == 0) {
				if (cp < minCp)
					throw new IllegalArgumentException("Invalid UTF-8 overlong sequence " + Integer.toString(cp, 16) + " < " + Integer.toString(minCp, 16));
				if (cp > 0xFFFF) { // Surrogate pair
					cp -= 0x10000;
					carr[cpos++] = (char) ((cp >> 10) ^ 0xD800);
					carr[cpos++] = (char) ((cp & 0x3FF) ^ 0xDC00);
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

	private static int continuation(int cp, int b) {
		if ((b & 0xc0) != 0x80)
			throw new IllegalArgumentException("Invalid UTF-8 continuation byte");
		return (cp << 6) ^ (b & 0x3F);
	}

	private static void updatePos(ByteBuffer bbuf, int bpos, CharBuffer cbuf, int cpos) {
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

			cp = (cp << 6) | (b & 0x3F);
			if ((r == 2 && cp < 0x20) || (r == 3 && cp < 0x10))
				throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
			if (--r == 0)
				slowAppend(cbuf);
		} else if (b < 0x80) // 1-byte ASCII
			cbuf.put((char) b);
		else if (b < 0xc0)
			throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b, 16));
		else if (b < 0xe0) {
			r = 1;
			cp = b & 0x1F;
			if (cp < 2)
				throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
		} else if (b < 0xf0) {
			r = 2;
			cp = b & 0x0F;
		} else if (b < 0xfb) {
			r = 3;
			cp = b & 0x07;
		} else
			throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b, 16));
	}

	private void slowAppend(CharBuffer cbuf) {
		if (cp > 0xFFFF) { // Surrogate pair
			cp -= 0x10000;
			cbuf.put((char) ((cp >> 10) + 0xD800)).put((char) ((cp & 0x3FF) + 0xDC00));
		} else
			cbuf.put((char) cp);
		cp = 0;
	}
}
