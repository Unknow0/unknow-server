package unknow.server.servlet.http2;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.BiConsumer;

import unknow.server.util.io.Buffers;

/**
 * Manage header encoded in HPACK
 * implements https://httpwg.org/specs/rfc7541.html
 */
public class Http2Headers {
	private static final int[] MASK = { 0b00000000, 0b00000001, 0b00000011, 0b00000111, 0b00001111, 0b00011111, 0b00111111, 0b01111111, 0b11111111 };
	private static final int[] MAX = { 0, 1, 3, 7, 15, 31, 63, 127, 255 };

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
	private int maxSize;

	public Http2Headers(int maxSize) {
		this.settingsSize = maxSize;
		this.maxSize = settingsSize;

		this.dynamic = new LinkedList<>();
		this.size = 0;
	}

	/**
	 * read an int
	 * @param in the data
	 * @param i the first byte
	 * @param n the prefix
	 * @return
	 * @throws IOException 
	 */
	public static int readInt(InputStream in, int i, int n) throws IOException {
		i = i & MASK[n];
		if (i == MAX[n]) {
			int m = 0;
			int j;
			do {
				j = in.read();
				if (j == -1)
					throw new EOFException();
				i += (j & 0x7F) << m;
				m += 7;
			} while ((j & 0x10) == 0x10);
		}
		return i;
	}

	/**
	* write an int
	* @param out the data
	* @param p the prefix value
	* @param n the prefix length
	* @param v the value
	* @throws InterruptedException 
	*/
	public static void writeInt(Buffers out, int p, int n, int v) throws InterruptedException {
		int m = MAX[n];
		if (v < m) {
			out.write(v | p);
			return;
		}
		out.write(p | m);
		v -= m;
		while (v >= 0x80) {
			out.write((v & 0x7F) | 0x80);
			v >>= 7;
		}
		out.write(v);
	}

	/**
	 * write a header to the output
	 * @param out the output
	 * @param name the header name
	 * @param value the header value
	 * @throws InterruptedException 
	 */
	public void writeHeader(Buffers out, String name, String value) throws InterruptedException {
		int o = -1;
		for (int i = 0; i < TABLE.length; i++) {
			Entry e = TABLE[i];
			if (e.name.equals(name)) {
				if (e.value != null && e.value.equals(value)) {
					writeInt(out, 0b10000000, 7, i + 1);
					return;
				}
				if (o == -1)
					o = i;
			}
		}
		int i = TABLE.length;
		for (Entry e : dynamic) {
			if (e.name.equals(name)) {
				if (e.value.equals(value)) {
					writeInt(out, 0b10000000, 7, i + 1);
					return;
				}
				if (o == -1)
					o = i;
			}
			i++;
		}

		writeInt(out, 0b01000000, 6, o < 0 ? 0 : o + 1);
		if (o < 0)
			writeData(out, name);
		writeData(out, value);
		add(new Entry(name, value));
	}

	/**
	 * read one header
	 * @param in the input
	 * @param h the header key/value
	 * @throws IOException in case of error
	 */
	public void readHeader(InputStream in, BiConsumer<String, String> h) throws IOException {
		int i = in.read();
		if (i == -1)
			throw new EOFException();

		if ((i & 0b10000000) != 0) { // indexed
			Entry e = get(readInt(in, i, 7));
			h.accept(e.name(), e.value());
		} else if ((i & 0b01000000) != 0) { // literal indexed
			i = readInt(in, i, 6);
			String n;
			if (i == 0) {  // new name
				n = readData(in);
			} else
				n = get(i).name;
			String v = readData(in);

			h.accept(n, v);
			add(new Entry(n, v));
		} else if ((i & 0b00100000) != 0) { // update size
			i = readInt(in, i, 5);
			if (i > settingsSize)
				throw new IOException("Update size > max size");
			maxSize = i;
			ensureMax();
		} else { // literal non indexed
			i = readInt(in, i, 4);
			String n;
			if (i == 0) {  // new name
				n = readData(in);
			} else
				n = get(i).name;
			String v = readData(in);
			h.accept(n, v);
		}
	}

	public void setMax(int size) {
		this.settingsSize = size;
		this.maxSize = Math.min(size, maxSize);
	}

	private static String readData(InputStream in) throws IOException {
		int i = in.read();
		if (i == -1)
			throw new EOFException();
		boolean huffman = (i & 0x80) == 0x80;
		i = readInt(in, i, 7);

		StringBuilder sb = new StringBuilder();
		if (!huffman)
			toString(sb, in, i);
		else
			Http2Huffman.decode(in, i, sb);
		return sb.toString();
	}

	private static void writeData(Buffers out, String value) throws InterruptedException {
		byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);

		Buffers b = new Buffers();
		Http2Huffman.encode(b, bytes);
		if (b.length() < bytes.length) {
			writeInt(out, 0x80, 7, b.length());
			b.read(out, -1, false);
		} else {
			writeInt(out, 0, 7, bytes.length);
			out.write(bytes);
		}
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
		while (size > maxSize && (e = dynamic.pollLast()) != null)
			size -= e.size();
	}

	public static void toString(StringBuilder sb, InputStream in, int l) throws IOException {
		while (l-- > 0) {
			int i = in.read();
			if (i < 0)
				throw new EOFException();
			sb.append((char) i);
		}
	}

	public static class Entry {
		final String name;
		final String value;

		public Entry(String name, String value) {
			this.name = name;
			this.value = value;
		}

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
}
