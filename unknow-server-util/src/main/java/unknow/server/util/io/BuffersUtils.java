/**
 * 
 */
package unknow.server.util.io;

import unknow.server.util.io.Buffers.Walker;

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
	 * @throws InterruptedException
	 */
	public static boolean startsWith(Buffers buf, byte[] lookup, int o, int l) throws InterruptedException {
		StartWith w = new StartWith(lookup);
		buf.walk(w, o, l);
		return w.found;
	}

	private static final class StartWith implements Walker {
		private final byte[] lookup;
		int i = 0;
		boolean found;

		public StartWith(byte[] lookup) {
			this.lookup = lookup;
			found = false;
		}

		@Override
		public boolean apply(byte[] b, int o, int e) {
			while (o < e && i < lookup.length) {
				if (lookup[i] != b[o])
					return false;
				i++;
				o++;
			}
			found = i == lookup.length;
			return !found;
		}
	}

	/**
	 * get index of
	 * 
	 * @param buf    buff to look into
	 * @param lookup data too lookup
	 * @param o      first index to check
	 * @param l      max number of bytes to check
	 * @return the index or -1 if not found, -2 if max reached
	 * @throws InterruptedException
	 */
	public static int indexOf(Buffers buf, byte lookup, int o, int l) throws InterruptedException {
		IndexOf w = new IndexOf(lookup);
		switch (buf.walk(w, o, l)) {
			case MAX:
				return -2;
			case STOPED:
				return w.i + o;
			default:
				return -1;
		}
	}

	private static final class IndexOf implements Walker {
		private final byte lookup;
		int i = 0;

		public IndexOf(byte lookup) {
			this.lookup = lookup;
		}

		@Override
		public boolean apply(byte[] b, int o, int e) {
			while (o < e) {
				if (lookup == b[o])
					return false;
				i++;
				o++;
			}
			return true;
		}
	}

	/**
	 * get first index of one of the lookup bytes
	 * 
	 * @param buf    buff to look into
	 * @param lookup data too lookup
	 * @param o      first index to check
	 * @param l      max number of bytes to check
	 * @return the index or -1 if not found, -2 if max reached
	 * @throws InterruptedException
	 */
	public static int indexOfOne(Buffers buf, byte[] lookup, int o, int l) throws InterruptedException {
		IndexOfOne w = new IndexOfOne(lookup);
		switch (buf.walk(w, o, l)) {
			case MAX:
				return -2;
			case STOPED:
				return w.i + o;
			default:
				return -1;
		}
	}

	private static final class IndexOfOne implements Walker {
		private final byte[] lookup;
		int i = 0;

		public IndexOfOne(byte[] lookup) {
			this.lookup = lookup;
		}

		@Override
		public boolean apply(byte[] b, int o, int e) {
			while (o < e) {
				for (int i = 0; i < lookup.length; i++) {
					if (lookup[i] == b[o])
						return false;
				}
				i++;
				o++;
			}
			return true;
		}
	}

	/**
	 * get index of
	 * 
	 * @param buf    buff to look into
	 * @param lookup data too lookup
	 * @return the index or -1 if not found, -2 if max reached
	 * @throws InterruptedException
	 */
	public static int indexOf(Buffers buf, byte[] lookup, int o, int l) throws InterruptedException {
		IndexOfBloc w = new IndexOfBloc(lookup);
		switch (buf.walk(w, o, l)) {
			case MAX:
				return -2;
			case STOPED:
				return w.r + o;
			default:
				return -1;
		}
	}

	private static final class IndexOfBloc implements Walker {
		private final byte[] lookup;
		private int i = 0;
		int r = 0;

		public IndexOfBloc(byte[] lookup) {
			this.lookup = lookup;
		}

		private final boolean match(byte[] b, int o, int e) {
			for (; i < lookup.length && o < e; i++, o++) {
				if (b[o] != lookup[i]) {
					i = 0;
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean apply(byte[] b, int o, int e) {
			while (o < e) {
				if (match(b, o, e))
					return i != lookup.length;
				o++;
				r++;
			}
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
	 * @throws InterruptedException
	 */
	public static void toString(StringBuilder sb, Buffers buf, int off, int len) throws InterruptedException {
		ToStringAscii w = new ToStringAscii(sb);
		buf.walk(w, off, len);
	}

	private static final class ToStringAscii implements Walker {
		private final StringBuilder sb;

		public ToStringAscii(StringBuilder sb) {
			this.sb = sb;
		}

		@Override
		public boolean apply(byte[] b, int o, int e) {
			while (o < e)
				sb.append((char) b[o++]);
			return true;
		}
	}
}