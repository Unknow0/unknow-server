package unknow.server.servlet.http2.header;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Http2HeadersEncoder extends Http2Headers {
	private ByteBuffer buf;

	public Http2HeadersEncoder(int maxSize) {
		super(maxSize);
		buf = ByteBuffer.allocate(8192);
	}

	private void put(byte b, ByteBufferConsumer c) throws IOException {
		buf.put(b);
		if (buf.hasRemaining())
			return;
		doFlush(c);
	}

	public void flush(ByteBufferConsumer c) throws IOException {
		if (buf.position() == 0)
			return;
		doFlush(c);
	}

	private void doFlush(ByteBufferConsumer c) throws IOException {
		c.accept(buf.flip());
		buf = ByteBuffer.allocate(8192);
	}

	/**
	 * write a header to the output
	 * @param name the header name
	 * @param value the header value
	 * @param c consumer of produced consumer
	 * @throws IOException  in case of ioexception
	 */
	public void encode(String name, String value, ByteBufferConsumer c) throws IOException {
		int o = -1;
		for (int i = 0; i < TABLE.length; i++) {
			Entry e = TABLE[i];
			if (e.name.equals(name)) {
				if (e.value != null && e.value.equals(value)) {
					encodeInt(0b10000000, 7, i + 1, c);
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
					encodeInt(0b10000000, 7, i + 1, c);
					return;
				}
				if (o == -1)
					o = i;
			}
			i++;
		}

		encodeInt(0b01000000, 6, o < 0 ? 0 : o + 1, c);
		if (o < 0)
			writeData(name, c);
		writeData(value, c);
		add(new Entry(name, value));
	}

	/**
	* write an int
	* @param out the data
	* @param p the prefix value
	* @param n the prefix length
	* @param v the value
	* @throws IOException 
	*/
	private void encodeInt(int p, int n, int v, ByteBufferConsumer c) throws IOException {
		int m = MAX[n];
		if (v < m) {
			put((byte) (v | p), c);
			return;
		}
		put((byte) (p | m), c);
		v -= m;
		while (v >= 0x80) {
			put((byte) ((v & 0x7F) | 0x80), c);
			v >>= 7;
		}
		put((byte) v, c);
	}

	private void writeData(String value, ByteBufferConsumer c) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
		if (buf.remaining() < bytes.length)
			flush(c);

		ByteBuffer b = ByteBuffer.allocate(bytes.length);
		if (Http2Huffman.encode(b, bytes)) {
			encodeInt(0x80, 7, b.position(), c);
			int l = b.flip().limit();
			while (b.hasRemaining()) {
				buf.put(b.limit(Math.min(l, buf.remaining())));
				if (!buf.hasRemaining())
					doFlush(c);
			}
		} else {
			encodeInt(0, 7, bytes.length, c);
			int o = 0;
			while (o < bytes.length) {
				int l = Math.min(buf.remaining(), bytes.length);
				buf.put(bytes, o, l);
				if (!buf.hasRemaining())
					doFlush(c);
			}
		}
	}

	public static interface ByteBufferConsumer {
		void accept(ByteBuffer b) throws IOException;
	}
}
