package unknow.server.servlet.http2;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.BiConsumer;

import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersUtils;

/**
 * Manage header encoded in HPACK
 * implements https://httpwg.org/specs/rfc7541.html
 */
public class Http2Headers {
	protected static final Entry[] TABLE = new Entry[] { //@formatter:off
			new Entry(":authority", null),
			new Entry(":method", "GET"),
			new Entry(":method", "POST"),
			new Entry(":path", "/"),
			new Entry(":path", "/index.html"),
			new Entry(":scheme", "http"),
			new Entry(":scheme", "https"),
			new Entry(":status", "200"),
			new Entry(":status", "204"),
			new Entry(":status", "206"),
			new Entry(":status", "304"),
			new Entry(":status", "400"),
			new Entry(":status", "404"),
			new Entry(":status", "500"),
			new Entry("accept-charset", null),
			new Entry("accept-encoding", "gzip,deflate"),
			new Entry("accept-language", null),
			new Entry("accept-ranges", null),
			new Entry("accept", null),
			new Entry("access-control-allow-origin", null),
			new Entry("age", null),
			new Entry("allow", null),
			new Entry("authorization", null),
			new Entry("cache-control", null),
			new Entry("content-disposition", null),
			new Entry("content-encoding", null),
			new Entry("content-language", null),
			new Entry("content-length", null),
			new Entry("content-location", null),
			new Entry("content-range", null),
			new Entry("content-type", null),
			new Entry("cookie", null),
			new Entry("date", null),
			new Entry("etag", null),
			new Entry("expect", null),
			new Entry("expires", null),
			new Entry("from", null),
			new Entry("host", null),
			new Entry("if-match", null),
			new Entry("if-modified-since", null),
			new Entry("if-none-match", null),
			new Entry("if-range", null),
			new Entry("if-unmodified-since", null),
			new Entry("last-modified", null),
			new Entry("link", null),
			new Entry("location", null),
			new Entry("max-forwards", null),
			new Entry("proxy-authenticate", null),
			new Entry("proxy-authorization", null),
			new Entry("range", null),
			new Entry("referer", null),
			new Entry("refresh", null),
			new Entry("retry-after", null),
			new Entry("server", null),
			new Entry("set-cookie", null),
			new Entry("strict-transport-security", null),
			new Entry("transfer-encoding", null),
			new Entry("user-agent", null),
			new Entry("vary", null),
			new Entry("via", null),
			new Entry("www-authenticate", null)
			};//@formatter:on
	protected final LinkedList<Entry> dynamic;

	private int settingsSize;
	private int size;
	private int max;

	public Http2Headers(int maxSize) {
		this.settingsSize = maxSize;
		this.max = settingsSize;

		this.dynamic = new LinkedList<>();
		this.size = 0;
	}

	public void readHeaders(Buffers b, BiConsumer<String, String> h) throws InterruptedException, IOException {
		while (!b.isEmpty())
			readHeader(b, h);
	}

	private void readHeader(Buffers b, BiConsumer<String, String> h) throws InterruptedException, IOException {
		int i = b.read(false);
		if (i == -1)
			throw new IOException("EOF");

		if ((i & 0b10000000) != 0) { // indexed
			Entry e = get(readInt(b, i, 7));
			h.accept(e.name(), e.value());
		} else if ((i & 0b01000000) != 0) { // literal indexed
			i = readInt(b, i, 6);
			String n;
			if (i == 0) {  // new name
				n = readData(b);
			} else
				n = get(i).name;
			String v = readData(b);

			h.accept(n, v);
			add(new Entry(n, v));
		} else if ((i & 0b00100000) != 0) { // update size
			i = readInt(b, i, 5);
			if (i > settingsSize)
				; // TODO error
			max = i;
			ensureMax();
		} else { // literal non indexed
			i = readInt(b, i, 4);
			String n;
			if (i == 0) {  // new name
				n = readData(b);
			} else
				n = get(i).name;
			String v = readData(b);
			h.accept(n, v);
		}
	}

	public void setMax(int size) {
		this.settingsSize = size;
		this.max = Math.min(size, max);
	}

	private String readData(Buffers b) throws InterruptedException, IOException {
		int i = b.read(false);
		if (i == -1)
			throw new IOException("EOF");
		boolean huffman = (i & 0x80) == 0x80;
		i = readInt(b, i, 7);

		StringBuilder sb = new StringBuilder();
		if (!huffman) {
			BuffersUtils.toString(sb, b, 0, i);
			b.skip(i);
		} else
			Http2Huffman.decode(b, i, sb);
//		return new EntryData(sb.toString(), i);
		return sb.toString();
	}

	protected Entry get(int i) {
		if (--i < TABLE.length)
			return TABLE[i];
		return dynamic.get(i - TABLE.length);
	}

	protected void add(Entry e) {
		size += e.size();
		dynamic.addFirst(e);
		ensureMax();
	}

	protected void ensureMax() {
		Entry e;
		while (size > max && (e = dynamic.pollLast()) != null)
			size -= e.size();
	}

	/**
	 * read an int
	 * @param b the data
	 * @param i the first byte
	 * @param n the prefix
	 * @return
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public static int readInt(Buffers b, int i, int n) throws InterruptedException, IOException {
		i = i & MASK[n];
		if (i == MAX[n]) {
			int m = 0;
			int j;
			do {
				j = b.read(false);
				if (j == -1)
					throw new IOException("EOF");
				i += (j & 0x7F) << m;
				m += 7;
			} while ((j & 0x10) == 0x10);
		}
		return i;
	}

	private static final int[] MASK = { 0b00000000, 0b00000001, 0b00000011, 0b00000111, 0b00001111, 0b00011111, 0b00111111, 0b01111111, 0b11111111 };
	private static final int[] MAX = { 0, 1, 3, 7, 15, 31, 63, 127, 255 };

	public static class Entry {
		final String name;
		final String value;

		public Entry(String name, String value) {
			this.name = name;
			this.value = value;
		}

//		public Entry(String name, String value) {
//			this(new EntryData(name, name.length()), value == null ? null : new EntryData(value, value.length()));
//		}

		public int size() {
			return (value == null ? name.length() : name.length() + value.length()) + 32;
		}

		public String name() {
			return name;
		}

		public String value() {
			return value == null ? null : value;
		}

		@Override
		public String toString() {
			return name + ": " + value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, value);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Entry other = (Entry) obj;
			return Objects.equals(name, other.name) && Objects.equals(value, other.value);
		}
	}

	public static class EntryData {
		final String v;
		final int l;

		public EntryData(String text, int len) {
			this.v = text;
			this.l = len;
		}

		@Override
		public String toString() {
			return v + "(" + l + ")";
		}

		@Override
		public int hashCode() {
			return Objects.hash(l, v);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EntryData other = (EntryData) obj;
			return l == other.l && Objects.equals(v, other.v);
		}
	}
}
