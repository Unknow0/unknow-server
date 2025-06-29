package unknow.server.servlet.utils;

import java.nio.ByteBuffer;

/**
 * encode string to utf8 bytes
 */
public class Utf8Encoder {
	private final CharSequence str;
	private final byte[] tmp;
	private final int l;
	private int count;
	private int i;
	private int r;

	/**
	 * new encoder
	 * @param str string to encode
	 */
	public Utf8Encoder(CharSequence str) {
		this.str = str;
		this.l = str.length();
		this.tmp = new byte[3];
	}

	/**
	 * check if the encoder has more bytes
	 * @return true if next bytes available
	 */
	public boolean hasNext() {
		return i < l || r > 0;
	}

	/**
	 * @return next encoded byte
	 */
	public byte next() {
		if (r > 0) {
			count++;
			return tmp[--r];
		}

		int code = str.charAt(i++);
		if (code >= 0xD800 && code <= 0xDBFF) {
			if (i < l)
				code = ((code << 10) + str.charAt(i++)) + (0x10000 - (0xD800 << 10) - 0xDC00);
			else
				code = 0xFFFD;
		}

		count++;
		if (code <= 0x7F)
			return (byte) code;
		else if (code <= 0x7FF) {
			tmp[r++] = (byte) (0x80 | (code & 0x3F));
			return (byte) (0xC0 | (code >> 6));
		}
		if (code <= 0xFFFF) {
			tmp[r++] = (byte) (0x80 | (0x80 | (code & 0x3F)));
			tmp[r++] = (byte) (0x80 | ((code >> 6) & 0x3F));
			return (byte) (0xE0 | (code >> 12));
		}
		tmp[r++] = (byte) (0x80 | (code & 0x3F));
		tmp[r++] = (byte) (0x80 | ((code >> 6) & 0x3F));
		tmp[r++] = (byte) (0x80 | ((code >> 12) & 0x3F));
		return (byte) (0xF0 | (code >> 18));
	}

	/**
	* @return the number of encoded bytes
	*/
	public int count() {
		return count;
	}

	/**
	 * calculate the length of the encoded string
	 * @param str the string
	 * @return the length of the encoded string
	 */
	public static int length(String str) {
		int i = 0;
		int l = str.length();
		int count = 0;
		while (i < l) {
			int code = str.charAt(i++);
			if (code >= 0xD800 && code <= 0xDBFF) {
				if (i < l)
					code = ((code << 10) + str.charAt(i++)) + (0x10000 - (0xD800 << 10) - 0xDC00);
				else
					code = 0xFFFD;
			}

			if (code <= 0x7F)
				count++;
			else if (code <= 0x7FF)
				count += 2;
			else if (code <= 0xFFFF)
				count += 3;
			else
				count += 4;
		}
		return count;
	}

	/**
	 * encode into a ByteBuffer
	 * @param str string to encode
	 * @param b encoded value
	 * @return true if the whole string is encoded
	 */
	public static boolean encode(String str, ByteBuffer b) {
		Utf8Encoder e = new Utf8Encoder(str);
		byte[] a = b.array();
		int i = b.position();
		int l = b.limit();
		while (e.hasNext() && i < l)
			a[i++] = e.next();
		return i <= l;
	}
}
