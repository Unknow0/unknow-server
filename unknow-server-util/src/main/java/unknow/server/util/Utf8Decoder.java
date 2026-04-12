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
			carr[cpos] = (char) l;
			carr[cpos + 1] = (char) (l >>> 8);
			carr[cpos + 2] = (char) (l >>> 16);
			carr[cpos + 3] = (char) (l >>> 24);
			carr[cpos + 4] = (char) (l >>> 32);
			carr[cpos + 5] = (char) (l >>> 40);
			carr[cpos + 6] = (char) (l >>> 48);
			carr[cpos + 7] = (char) (l >>> 56);
			bpos += 8;
			cpos += 8;
		}
		int cp;
		while (bpos < blim && cpos < clim) {
			int b = barr[bpos++];

			if (b >= 0)
				carr[cpos++] = (char) b;
			else if (b < -64)
				error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 start byte: 0x" + Integer.toString(b & 0xFF, 16));
			else if (b < -32) {
				cp = b & 0x1F;
				if (bpos == blim) {
					this.cp = cp;
					r = 1;
					minCp = 0x80;
					updatePos(bbuf, bpos, cbuf, cpos);
					return;
				}
				b = barr[bpos++];
				if ((b & 0xc0) != 0x80)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 continuation byte");
				cp = (cp << 6) ^ (b & 0x3F);
				if (cp < 0x80)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 overlong sequence");
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
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 continuation byte");

				cp = (cp << 12) ^ ((s & 0x3F) << 6) ^ ((s >> 8) & 0x3F);
				if (cp < 0x800)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 overlong sequence");
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
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 continuation byte");
				cp = (cp << 18) ^ ((i & 0x3F) << 12) ^ (((i >> 8) & 0x3F) << 6) ^ (((i >> 16) & 0x3F));
				if (cp < 0x10000)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 overlong sequence");
				if (cp > 0xFFFF) { // Surrogate pair
					cp -= 0x10000;
					carr[cpos++] = (char) ((cp >> 10) ^ 0xD800);
					carr[cpos++] = (char) ((cp & 0x3FF) ^ 0xDC00);
				} else
					carr[cpos++] = (char) cp;
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
			cp = (cp << 6) ^ (b & 0x3F);
			if (--r == 0) {
				if (cp < minCp)
					error(bbuf, bpos, cbuf, cpos, "Invalid UTF-8 overlong sequence " + Integer.toString(cp, 16) + " < " + Integer.toString(minCp, 16));
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

	private static void error(ByteBuffer bbuf, int bpos, CharBuffer cbuf, int cpos, String msg) {
		updatePos(bbuf, bpos, cbuf, cpos);
		throw new IllegalArgumentException(msg);
	}

	private static void updatePos(ByteBuffer bbuf, int bpos, CharBuffer cbuf, int cpos) {
		bbuf.position(bpos - bbuf.arrayOffset());
		cbuf.position(cpos - cbuf.arrayOffset());
	}

	private final char[] CARR = new char[8];

	private void slowDecode(ByteBuffer bbuf, CharBuffer cbuf) {
		if (r > 0 && slowRemaining(bbuf, cbuf, cp, r))
			return;

		int max = Math.min(bbuf.limit(), bbuf.position() + cbuf.remaining()) - 8;
		int bpos = bbuf.position();
		while (bpos < max) {
			long l = bbuf.getLong(bpos);
			if ((l & 0x8080808080808080L) != 0L)
				break;
			CARR[0] = (char) l;
			CARR[1] = (char) (l >>> 8);
			CARR[2] = (char) (l >>> 16);
			CARR[3] = (char) (l >>> 24);
			CARR[4] = (char) (l >>> 32);
			CARR[5] = (char) (l >>> 40);
			CARR[6] = (char) (l >>> 48);
			CARR[7] = (char) (l >>> 56);
			cbuf.put(CARR);
			bpos += 8;
		}
		int cp;
		while (bbuf.hasRemaining() && cbuf.remaining() > 1) {
			int b = bbuf.get();
			if (b >= 0)
				cbuf.put((char) b);
			else if (b < -64)
				throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b & 0xFF, 16));
			else if (b < -32) {
				cp = b & 0x1F;
				if (!bbuf.hasRemaining()) {
					minCp = 0x80;
					r = 1;
					this.cp = cp;
					return;
				}
				b = bbuf.get();
				if ((b & 0xc0) != 0x80)
					throw new IllegalArgumentException("Invalid UTF-8 continuation byte");
				cp = (cp << 6) ^ (b & 0x3F);
				if (cp < 0x80)
					throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
				cbuf.put((char) cp);
			} else if (b < -16) {
				cp = b & 0x0F;
				if (bbuf.remaining() < 2) {
					minCp = 0x800;
					slowRemaining(bbuf, cbuf, cp, 2);
					return;
				}
				short s = bbuf.getShort();
				if ((s & 0xc0c0) != 0x8080)
					throw new IllegalArgumentException("Invalid UTF-8 continuation byte");

				cp = (cp << 12) ^ ((s & 0x3F) << 6) ^ ((s >> 8) & 0x3F);
				if (cp < 0x800)
					throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
				cbuf.put((char) cp);
			} else if (b < -8) {
				cp = b & 0x07;
				if (bbuf.remaining() < 4) {
					minCp = 0x10000;
					slowRemaining(bbuf, cbuf, cp, 3);
					return;
				}
				int i = bbuf.getInt(bbuf.position());
				bbuf.position(bbuf.position() + 3);
				if ((i & 0x00C0C0C0) != 0x00808080)
					throw new IllegalArgumentException("Invalid UTF-8 continuation byte");
				cp = (cp << 18) ^ ((i & 0x3F) << 12) ^ (((i >> 8) & 0x3F) << 6) ^ (((i >> 16) & 0x3F));
				if (cp < 0x10000)
					throw new IllegalArgumentException("Invalid UTF-8 overlong sequence");
				slowAppend(cbuf, cp);
			} else
				throw new IllegalArgumentException("Invalid UTF-8 start byte: 0x" + Integer.toString(b, 16));
		}
	}

	private boolean slowRemaining(ByteBuffer bbuf, CharBuffer cbuf, int cp, int r) {
		while (bbuf.hasRemaining() && cbuf.remaining() > 1) {
			int b = bbuf.get();
			if ((b & 0xc0) != 0x80)
				throw new IllegalArgumentException("Invalid UTF-8 continuation byte");
			cp = (cp << 6) ^ (b & 0x3F);
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
