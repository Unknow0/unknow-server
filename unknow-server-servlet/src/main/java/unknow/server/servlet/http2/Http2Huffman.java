package unknow.server.servlet.http2;

import java.io.IOException;

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

	private static int bits(S s, int need) throws InterruptedException, IOException {
		int val = s.bit;
		while (s.cnt < need) {
			if (--s.max < 0) {
				if (s.bit != (1 << s.cnt) - 1)
					throw new IOException("EOF");
				return -1;
			}

			val <<= 8;
			val |= s.b.read(false); /* load eight bits */
			s.cnt += 8;
		}

		int m = ((1 << need) - 1) << (s.cnt - need);
		/* drop need bits and update buffer, always zero to seven bits left */
		s.cnt -= need;
		s.bit = val & ((1 << s.cnt) - 1);

		/* return need bits, zeroing the bits above that */
		return (val & m) >> s.cnt;
	}

	public static String decode(Buffers b, int max, StringBuilder sb) throws InterruptedException, IOException {
		S s = new S(b, max);
		char c;
		while ((s.max > 0 || s.cnt > 0) && (c = read(s)) != 256)
			sb.append(c);
		return sb.toString();
	}

	private static final char read(S s) throws InterruptedException, IOException {
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

	static final class S {
		final Buffers b;
		int max;

		int bit;
		int cnt;

		public S(Buffers b, int max) {
			this.b = b;
			this.max = max;
		}
	}

	public static void main(String[] arg) throws Exception {
//		System.out.println(
		Buffers b = new Buffers();
		b.write(0xae);
		b.write(0xc3);
		b.write(0x77);
		b.write(0x1a);
		b.write(0x4b);

		b.walk((d, o, l) -> {
			for (int i = 0; i < l; i++)
				System.out.println("	" + Integer.toString(d[i + o] & 0xFF, 2));
			return true;
		}, 0, -1);

		StringBuilder sb = new StringBuilder();
		decode(b, b.length(), sb);
		System.out.println(sb);
	}
}
