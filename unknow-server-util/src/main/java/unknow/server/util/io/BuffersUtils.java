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
	 * @param o      first index to check
	 * @param l      max number of bytes to check
	 * @return true if we stars with "lookup"
	 * @throws InterruptedException on interrupt
	 */
	public static boolean startsWith(Buffers buf, byte[] lookup, int o, int l) throws InterruptedException {
		StartWith w = new StartWith(lookup);
		buf.walk(w, o, l);
		return w.found();
	}

	/** walker to check if buffers start with */
	public static final class StartWith implements Walker {
		private final byte[] lookup;
		private int i = 0;
		private boolean found;

		/**
		 * create a new StartWith
		 * @param lookup the data to check
		 */
		public StartWith(byte[] lookup) {
			this.lookup = lookup;
			found = false;
		}

		/** @return true if startWith */
		public boolean found() {
			return found;
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
	 * @throws InterruptedException on interrupt
	 */
	public static int indexOf(Buffers buf, byte lookup, int o, int l) throws InterruptedException {
		IndexOf w = new IndexOf(lookup);
		switch (buf.walk(w, o, l)) {
			case MAX:
				return -2;
			case STOPED:
				return w.index() + o;
			default:
				return -1;
		}
	}

	/** walker to check if buffers start with */
	public static final class IndexOf implements Walker {
		private final byte lookup;
		private int i = 0;

		/** @param lookup byte to search */
		public IndexOf(byte lookup) {
			this.lookup = lookup;
		}

		/** @return the index found */
		public int index() {
			return i;
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
	 * @throws InterruptedException on interrupt
	 */
	public static int indexOfOne(Buffers buf, byte[] lookup, int o, int l) throws InterruptedException {
		IndexOfOne w = new IndexOfOne(lookup);
		switch (buf.walk(w, o, l)) {
			case MAX:
				return -2;
			case STOPED:
				return w.index() + o;
			default:
				return -1;
		}
	}

	/** walker to find indexOf one */
	public static final class IndexOfOne implements Walker {
		private final byte[] lookup;
		private int i = 0;

		/** @param lookup bytes to search */
		public IndexOfOne(byte[] lookup) {
			this.lookup = lookup;
		}

		/** @return the index found */
		public int index() {
			return i;
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
	 * @param o      first index to check
	 * @param l      max number of bytes to check
	 * @return the index or -1 if not found, -2 if max reached
	 * @throws InterruptedException on interrupt
	 */
	public static int indexOf(Buffers buf, byte[] lookup, int o, int l) throws InterruptedException {
		IndexOfBloc w = new IndexOfBloc(lookup);
		switch (buf.walk(w, o, l)) {
			case MAX:
				return -2;
			case STOPED:
				return w.index() + o;
			default:
				return -1;
		}
	}

	/** walker get get indexOf */
	public static final class IndexOfBloc implements Walker {
		private final byte[] lookup;
		private int i = 0;
		private int r = 0;

		/** @param lookup bytes to search */
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

		/** reset the search */
		public void reset() {
			r = 0;
			i = 0;
		}

		/** @return the found index */
		public int index() {
			return r;
		}
	}

	/**
	 * convert a buffer to a string assuming ascii encoding
	 * 
	 * @param sb  output buffer
	 * @param buf the buffer to convert
	 * @param off starting offset
	 * @param len length (-1 to take the full buffer)
	 * @throws InterruptedException on interrupt
	 */
	public static void toString(StringBuilder sb, Buffers buf, int off, int len) throws InterruptedException {
		ToStringAscii w = new ToStringAscii(sb);
		buf.walk(w, off, len);
	}

	/** walker to ascii string */
	public static final class ToStringAscii implements Walker {
		private final StringBuilder sb;

		/** @param sb StringBuilder to append */
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

	/**
	 * convert a buffers to a byte array
	 * 
	 * @param buf the buffer to convert
	 * @param o      first index to check
	 * @param l      max number of bytes to check
	 * @return the bytes
	 * @throws InterruptedException on interrupt
	 */
	public static byte[] toArray(Buffers buf, int o, int l) throws InterruptedException {
		ToArray w = new ToArray(l < 0 ? buf.length() : Math.min(buf.length(), l));
		buf.walk(w, o, l);
		return w.b;
	}

	/** walker to convert to array */
	public static final class ToArray implements Walker {
		private final byte[] b;
		private int i;

		/** @param l array size */
		public ToArray(int l) {
			b = new byte[l];
		}

		/** @return the bytes */
		public byte[] getByte() {
			return b;
		}

		@Override
		public boolean apply(byte[] b, int o, int e) {
			System.arraycopy(b, o, this.b, i, e);
			i += e;
			return true;
		}
	}
}