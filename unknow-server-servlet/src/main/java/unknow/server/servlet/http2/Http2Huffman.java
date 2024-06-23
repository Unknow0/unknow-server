package unknow.server.servlet.http2;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import unknow.server.util.io.Buffers;

/**
 * HPACK static huffman table
 * https://httpwg.org/specs/rfc7541.html#huffman.code
 * https://github.com/madler/zlib/blob/master/contrib/puff/puff.c
 */
public class Http2Huffman {
	private static final int MINBITS = 5;
	private static final int MAXBITS = 30;

	private static final short[] counts = { 0, 0, 0, 0, 0, 10, 26, 32, 6, 0, 5, 3, 2, 6, 2, 3, 0, 0, 0, 3, 8, 13, 26, 29, 12, 4, 15, 19, 29, 0, 4 };
	private static final char[] symbols = { 48, 49, 50, 97, 99, 101, 105, 111, 115, 116, // 5
			32, 37, 45, 46, 47, 51, 52, 53, 54, 55, 56, 57, 61, 65, 95, 98, 100, 102, 103, 104, 108, 109, 110, 112, 114, 117, // 6
			58, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 89, 106, 107, 113, 118, 119, 120, 121, 122, // 7
			38, 42, 44, 59, 88, 90, // 8
			33, 34, 40, 41, 63, // 10
			39, 43, 124, // 11
			35, 62, // 12
			0, 36, 64, 91, 93, 126, // 13
			94, 125, // 14
			60, 96, 123, // 15
			92, 195, 208, // 19
			128, 130, 131, 162, 184, 194, 224, 226, // 20
			153, 161, 167, 172, 176, 177, 179, 209, 216, 217, 227, 229, 230, // 21
			129, 132, 133, 134, 136, 146, 154, 156, 160, 163, 164, 169, 170, 173, 178, 181, 185, 186, 187, 189, 190, 196, 198, 228, 232, 233, // 22
			1, 135, 137, 138, 139, 140, 141, 143, 147, 149, 150, 151, 152, 155, 157, 158, 165, 166, 168, 174, 175, 180, 182, 183, 188, 191, 197, 231, 239, //23
			9, 142, 144, 145, 148, 159, 171, 206, 215, 225, 236, 237, // 24
			199, 207, 234, 235, // 25
			192, 193, 200, 201, 202, 205, 210, 213, 218, 219, 238, 240, 242, 243, 255, // 26
			203, 204, 211, 212, 214, 221, 222, 223, 241, 244, 245, 246, 247, 248, 250, 251, 252, 253, 254, // 27
			2, 3, 4, 5, 6, 7, 8, 11, 12, 14, 15, 16, 17, 18, 19, 20, 21, 23, 24, 25, 26, 27, 28, 29, 30, 31, 127, 220, 249, // 28
			10, 13, 22, 256 // 29, 30
	};

	private static final int[] codes = { 0x1ff8, 0x7fffd8, 0xfffffe2, 0xfffffe3, 0xfffffe4, 0xfffffe5, 0xfffffe6, 0xfffffe7, 0xfffffe8, 0xffffea, 0x3ffffffc, 0xfffffe9,
			0xfffffea, 0x3ffffffd, 0xfffffeb, 0xfffffec, 0xfffffed, 0xfffffee, 0xfffffef, 0xffffff0, 0xffffff1, 0xffffff2, 0x3ffffffe, 0xffffff3, 0xffffff4, 0xffffff5,
			0xffffff6, 0xffffff7, 0xffffff8, 0xffffff9, 0xffffffa, 0xffffffb, 0x14, 0x3f8, 0x3f9, 0xffa, 0x1ff9, 0x15, 0xf8, 0x7fa, 0x3fa, 0x3fb, 0xf9, 0x7fb, 0xfa, 0x16,
			0x17, 0x18, 0x0, 0x1, 0x2, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x5c, 0xfb, 0x7ffc, 0x20, 0xffb, 0x3fc, 0x1ffa, 0x21, 0x5d, 0x5e, 0x5f, 0x60, 0x61, 0x62,
			0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f, 0x70, 0x71, 0x72, 0xfc, 0x73, 0xfd, 0x1ffb, 0x7fff0, 0x1ffc, 0x3ffc, 0x22, 0x7ffd,
			0x3, 0x23, 0x4, 0x24, 0x5, 0x25, 0x26, 0x27, 0x6, 0x74, 0x75, 0x28, 0x29, 0x2a, 0x7, 0x2b, 0x76, 0x2c, 0x8, 0x9, 0x2d, 0x77, 0x78, 0x79, 0x7a, 0x7b, 0x7ffe, 0x7fc,
			0x3ffd, 0x1ffd, 0xffffffc, 0xfffe6, 0x3fffd2, 0xfffe7, 0xfffe8, 0x3fffd3, 0x3fffd4, 0x3fffd5, 0x7fffd9, 0x3fffd6, 0x7fffda, 0x7fffdb, 0x7fffdc, 0x7fffdd, 0x7fffde,
			0xffffeb, 0x7fffdf, 0xffffec, 0xffffed, 0x3fffd7, 0x7fffe0, 0xffffee, 0x7fffe1, 0x7fffe2, 0x7fffe3, 0x7fffe4, 0x1fffdc, 0x3fffd8, 0x7fffe5, 0x3fffd9, 0x7fffe6,
			0x7fffe7, 0xffffef, 0x3fffda, 0x1fffdd, 0xfffe9, 0x3fffdb, 0x3fffdc, 0x7fffe8, 0x7fffe9, 0x1fffde, 0x7fffea, 0x3fffdd, 0x3fffde, 0xfffff0, 0x1fffdf, 0x3fffdf,
			0x7fffeb, 0x7fffec, 0x1fffe0, 0x1fffe1, 0x3fffe0, 0x1fffe2, 0x7fffed, 0x3fffe1, 0x7fffee, 0x7fffef, 0xfffea, 0x3fffe2, 0x3fffe3, 0x3fffe4, 0x7ffff0, 0x3fffe5,
			0x3fffe6, 0x7ffff1, 0x3ffffe0, 0x3ffffe1, 0xfffeb, 0x7fff1, 0x3fffe7, 0x7ffff2, 0x3fffe8, 0x1ffffec, 0x3ffffe2, 0x3ffffe3, 0x3ffffe4, 0x7ffffde, 0x7ffffdf,
			0x3ffffe5, 0xfffff1, 0x1ffffed, 0x7fff2, 0x1fffe3, 0x3ffffe6, 0x7ffffe0, 0x7ffffe1, 0x3ffffe7, 0x7ffffe2, 0xfffff2, 0x1fffe4, 0x1fffe5, 0x3ffffe8, 0x3ffffe9,
			0xffffffd, 0x7ffffe3, 0x7ffffe4, 0x7ffffe5, 0xfffec, 0xfffff3, 0xfffed, 0x1fffe6, 0x3fffe9, 0x1fffe7, 0x1fffe8, 0x7ffff3, 0x3fffea, 0x3fffeb, 0x1ffffee, 0x1ffffef,
			0xfffff4, 0xfffff5, 0x3ffffea, 0x7ffff4, 0x3ffffeb, 0x7ffffe6, 0x3ffffec, 0x3ffffed, 0x7ffffe7, 0x7ffffe8, 0x7ffffe9, 0x7ffffea, 0x7ffffeb, 0xffffffe, 0x7ffffec,
			0x7ffffed, 0x7ffffee, 0x7ffffef, 0x7fffff0, 0x3ffffee };

	private static final int[] sizes = { 13, 23, 28, 28, 28, 28, 28, 28, 28, 24, 30, 28, 28, 30, 28, 28, 28, 28, 28, 28, 28, 28, 30, 28, 28, 28, 28, 28, 28, 28, 28, 28, 6, 10,
			10, 12, 13, 6, 8, 11, 10, 10, 8, 11, 8, 6, 6, 6, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 7, 8, 15, 6, 12, 10, 13, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 8, 7, 8, 13, 19, 13, 14, 6, 15, 5, 6, 5, 6, 5, 6, 6, 6, 5, 7, 7, 6, 6, 6, 5, 6, 7, 6, 5, 5, 6, 7, 7, 7, 7, 7, 15, 11, 14, 13, 28, 20, 22, 20, 20, 22, 22,
			22, 23, 22, 23, 23, 23, 23, 23, 24, 23, 24, 24, 22, 23, 24, 23, 23, 23, 23, 21, 22, 23, 22, 23, 23, 24, 22, 21, 20, 22, 22, 23, 23, 21, 23, 22, 22, 24, 21, 22, 23,
			23, 21, 21, 22, 21, 23, 22, 23, 23, 20, 22, 22, 22, 23, 22, 22, 23, 26, 26, 20, 19, 22, 23, 22, 25, 26, 26, 26, 27, 27, 26, 24, 25, 19, 21, 26, 27, 27, 26, 27, 24,
			21, 21, 26, 26, 28, 27, 27, 27, 20, 24, 20, 21, 22, 21, 21, 23, 22, 22, 25, 25, 24, 24, 26, 23, 26, 27, 26, 26, 27, 27, 27, 27, 27, 28, 27, 27, 27, 27, 27, 26 };

	private Http2Huffman() {
	}

	private static int bits(S s, int need) throws IOException {
		int val = s.bit;
		while (s.cnt < need) {
			if (--s.max < 0) {
				if (s.bit != (1 << s.cnt) - 1)
					throw new EOFException();
				return -1;
			}

			val <<= 8;
			val |= s.b.read(); /* load eight bits */
			s.cnt += 8;
		}

		int m = ((1 << need) - 1) << (s.cnt - need);
		/* drop need bits and update buffer, always zero to seven bits left */
		s.cnt -= need;
		s.bit = val & ((1 << s.cnt) - 1);

		/* return need bits, zeroing the bits above that */
		return (val & m) >> s.cnt;
	}

	public static String decode(InputStream b, int max, StringBuilder sb) throws IOException {
		S s = new S(b, max);
		char c;
		while ((s.max > 0 || s.cnt > 0) && (c = read(s)) != 256)
			sb.append(c);
		return sb.toString();
	}

	public static void encode(Buffers b, byte[] data) throws InterruptedException {
		C c = new C();
		for (int i = 0; i < data.length; i++)
			encode(c, b, data[i]);
		if (c.cnt != 0)
			b.write(c.bit | (0xff >> c.cnt));
	}

	private static void encode(C c, Buffers buf, byte b) throws InterruptedException {
		int code = codes[b];
		int size = sizes[b];

		while (size > 0) {
			int r = 8 - c.cnt;
			if (size <= r) {
				c.cnt += size;
				c.bit |= (code << (r - size)) & 0xFF;
				return;
			}

			size -= r;
			buf.write(c.bit | (code >> size) & 0xFF);
			c.bit = 0;
			c.cnt = 0;
		}
	}

	private static final char read(S s) throws IOException {
		int first = 0; /* first code of length len */
		int index = 0; /* index of first code of length len in symbol table */

		int len = MINBITS;
		int code = bits(s, 5);
		if (code == -1)
			return 256;
		while (true) {
			int count = counts[len];
			if (code - count < first) /* if length len, return symbol */
				return symbols[index + (code - first)];

			index += count; /* else update for next length */
			first += count;
			first <<= 1;
			code <<= 1;

			if (++len > MAXBITS)
				throw new IOException("Broken code");
			code |= bits(s, 1); /* get next bit */
			if (code == -1)
				return 256;
		}
	}

	static class C {
		int bit;
		int cnt;
	}

	static class S extends C {
		final InputStream b;
		int max;

		public S(InputStream b, int max) {
			this.b = b;
			this.max = max;
		}
	}
}
