package unknow.server.servlet.http2.header;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.servlet.utils.Utf8Encoder;
import unknow.server.util.ConsumerWithException;

public class Http2HeadersEncoder extends Http2Headers {
	private ByteBuffer buf;

	/**
	 * new encoder
	 * @param maxSize size of the header table
	 */
	public Http2HeadersEncoder(int maxSize) {
		super(maxSize);
		buf = ByteBuffer.allocate(8192);
	}

	private <E extends Throwable> void put(byte b, ConsumerWithException<ByteBuffer, E> c) throws E {
		buf.put(b);
		if (buf.hasRemaining())
			return;
		doFlush(c);
	}

	/**
	 * flush pending data
	 * @param <E> exception thown by the consumer
	 * @param c consumer of encoded header
	 * @throws E in case of error in the consumer
	 */
	public <E extends Throwable> void flush(ConsumerWithException<ByteBuffer, E> c) throws E {
		if (buf.position() == 0)
			return;
		doFlush(c);
	}

	private <E extends Throwable> void doFlush(ConsumerWithException<ByteBuffer, E> c) throws E {
		c.accept(buf.flip());
		buf = ByteBuffer.allocate(8192);
	}

	/**
	 * write a header to the output
	 * @param <E> exception of the consumer 
	 * @param name the header name
	 * @param value the header value
	 * @param c consumer of produced consumer
	 * @throws E in case of error in the consumer
	 */
	public <E extends Throwable> void encode(String name, String value, ConsumerWithException<ByteBuffer, E> c) throws E {
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
	private <E extends Throwable> void encodeInt(int p, int n, int v, ConsumerWithException<ByteBuffer, E> c) throws E {
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

	private <E extends Throwable> void writeData(String value, ConsumerWithException<ByteBuffer, E> c) throws E {
		ByteBuffer b = ByteBuffer.allocate(value.length() < 128 ? value.length() * 4 : Utf8Encoder.length(value));
		if (value.length() > 4 && Http2Huffman.encode(b, value))
			encodeInt(0x80, 7, b.position(), c);
		else {
			b.clear();
			Utf8Encoder.encode(value, b);
			encodeInt(0, 7, b.position(), c);
		}

		byte[] bytes = b.array();
		int len = b.position();
		int o = 0;
		while (o < bytes.length) {
			int l = Math.min(buf.remaining(), len);
			buf.put(bytes, o, l);
			if (!buf.hasRemaining())
				doFlush(c);
		}
	}
}
