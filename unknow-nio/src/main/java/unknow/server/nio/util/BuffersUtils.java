/**
 * 
 */
package unknow.server.nio.util;

import unknow.server.nio.util.Buffers.Chunk;

/**
 * @author unknow
 */
public final class BuffersUtils {
	private BuffersUtils() {
	}

	/**
	 * check if the data startsWith
	 * 
	 * @param buf    buff to look into
	 * @param lookup data too lookup
	 * @return true if we stars with "lookup"
	 */
	public static boolean startsWith(Buffers buf, byte[] lookup) {
		synchronized (buf) {
			Chunk b = buf.getHead();
			int l = lookup.length;
			if (b == null || buf.length() < l)
				return false;
			int i = 0;

			do {
				int e = b.o + b.l;
				for (int j = b.o; j < e && i < l; i++, j++) {
					if (lookup[i] != b.b[j])
						return false;
				}
			} while ((b = b.next) != null);
		}
		return true;
	}

	/**
	 * check if the data endsWith
	 * 
	 * @param buf    buff to look into
	 * @param lookup data too lookup
	 * @return true if we stars with "lookup"
	 */
	public static boolean endsWith(Buffers buf, byte[] lookup) {
		synchronized (buf) {
			Chunk b = buf.getHead();
			int l = lookup.length;
			int o = buf.length() - l;
			if (b == null || o < 0)
				return false;
			int i = 0;
			// skip
			while (o > b.l) {
				o -= b.l;
				b = b.next;
			}
			do {
				int e = b.o + b.l - o;
				for (int j = b.o + o; j < e && i < l; i++, j++) {
					if (lookup[i] != b.b[j])
						return false;
				}
				o = 0;
			} while ((b = b.next) != null);
		}
		return true;
	}

	/**
	 * check if the data equals
	 * 
	 * @param buf    buff to look into
	 * @param lookup data too lookup
	 * @return true if we stars with "lookup"
	 */
	public static boolean equals(Buffers buf, byte[] lookup) {
		synchronized (buf) {
			Chunk b = buf.getHead();
			int l = lookup.length;
			if (b == null || buf.length() != l)
				return false;
			int i = 0;
			int e, j;
			do {
				e = b.o + b.l;
				j = b.o;
				while (j < e && i < l) {
					if (lookup[i++] != b.b[j++])
						return false;
				}
				if (i == l)
					return true;
			} while ((b = b.next) != null);
		}
		return false;
	}

	/**
	 * check if the path equals or is followed by '/' or '?'
	 * 
	 * @param buf  buff to look into
	 * @param path data too lookup
	 * @return true if we stars with "lookup"
	 */
	public static boolean pathMatches(Buffers buf, byte[] path) {
		synchronized (buf) {
			Chunk b = buf.getHead();
			int l = path.length;
			if (b == null || buf.length() < l)
				return false;
			int i = 0;
			int e, j;
			do {
				e = b.o + b.l;
				j = b.o;
				while (j < e && i < l) {
					if (path[i++] != b.b[j++])
						return false;
				}
				if (i == l && j < e)
					return b.b[j] == '/' || b.b[j] == '?';
			} while ((b = b.next) != null);
			return true;
		}
	}

	/**
	 * convert a buffer to a string assuming ascii encoding
	 * 
	 * @param sb  output buffer
	 * @param buf the buffer to convert
	 * @param off starting offset
	 * @param len length (-1 to take the full buffer)
	 */
	public static void toString(StringBuilder sb, Buffers buf, int off, int len) {
		synchronized (buf) {
			Chunk b = buf.getHead();
			if (b == null)
				return;
			if (len == 0 || off >= buf.length())
				return;
			if (len == -1)
				len = buf.length() - off;
			sb.ensureCapacity(len);
			do {
				if (b.l < off) {
					off -= b.l;
					continue;
				}
				int w = b.l - off;
				for (int i = b.o + off; i < b.o + b.l; i++)
					sb.append((char) b.b[i]);
				off = 0;
				len -= w;
			} while (len > 0 && (b = b.next) != null);
		}
	}
}
