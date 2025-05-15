package unknow.server.servlet.utils;

public class Utf8Decoder {
	private final StringBuilder sb;
	private final boolean percent;

	/** code point in building */
	private int codePoint;
	/** remaing octect to read */
	private int r;

	/** hex char value */
	private int hex;
	/** remaining hex char to read */
	private int p;

	public Utf8Decoder() {
		this(new StringBuilder(), false);
	}

	public Utf8Decoder(boolean percent) {
		this(new StringBuilder(), percent);
	}

	public Utf8Decoder(StringBuilder sb, boolean percent) {
		this.sb = sb;
		this.percent = percent;
		this.codePoint = 0;
		this.r = 0;
	}

	public Utf8Decoder append(byte[] bytes, int i, int e) {
		while (i < e) {
			int b = bytes[i++] & 0xFF;

			if (p > 0) {
				int digit = Character.digit(b, 16);
				if (digit < 0)
					throw new IllegalArgumentException("Invalid percent encoding value");
				hex = (hex << 4 | digit);
				if (--p == 0) {
					append(hex);
					hex = 0;
				}
			} else if (percent && b == '%')
				p = 2;
			else if (r > 0)
				append(b);

			else
				append(b);
		}
		return this;
	}

	public void append(int b) {
		if (r == 0) {
			if ((b >> 7) == 0) // 1-byte ASCII
				sb.append((char) b);
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

		} else {
			if ((b >> 6) != 0b10)
				throw new IllegalArgumentException("Invalid UTF-8 continuation byte: " + b);

			codePoint = (codePoint << 6) | (b & 0x3F);
			if (--r == 0)
				append();
		}
	}

	private void append() {
		if (codePoint > 0xFFFF) { // Surrogate pair
			int cp = codePoint - 0x10000;
			sb.append((char) ((cp >> 10) + 0xD800)).append((char) ((cp & 0x3FF) + 0xDC00));
		} else
			sb.append((char) codePoint);
		codePoint = 0;
	}

	public String done() {
		if (r > 0)
			throw new IllegalArgumentException("Invalid UTF-8 string, truncated continuation bytes");
		if (p > 0)
			throw new IllegalArgumentException("Invalid percent encoded value, trucated encoding");
		String s = sb.toString();
		sb.setLength(0);
		return s;
	}
}
