package unknow.server.servlet.utils;

public class Utf8Decoder {
	private final StringBuilder sb;

	/** code point in building */
	private int codePoint;
	/** remaining octet to read */
	private int r;

	public Utf8Decoder() {
		this(new StringBuilder());
	}

	public Utf8Decoder(StringBuilder sb) {
		this.sb = sb;
		this.codePoint = 0;
	}

	public Utf8Decoder append(byte[] bytes, int i, int e) {
		while (i < e)
			append(bytes[i++] & 0xFF);
		return this;
	}

	public Utf8Decoder append(byte b) {
		append(b & 0xFF);
		return this;
	}

	private void append(int b) {
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
		String s = sb.toString();
		sb.setLength(0);
		return s;
	}
}
