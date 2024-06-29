package unknow.server.servlet.http2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import unknow.server.servlet.http2.Http2Headers.Entry;
import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersInputStream;

public class Http2HeadersTest {

	public void staticTable() {
		assertEquals(61, Http2Headers.TABLE.length);
	}

	public static Stream<Arguments> integer() {
		//@formatter:off
		return Stream.of(
				Arguments.of(new byte[] { b(0b00001010) }, 5, 10),
				Arguments.of(new byte[] { b(0b00011111), b(0b10011010), b(0b00001010) }, 5, 1337),
				Arguments.of(new byte[] { b(0b00101010) }, 8, 42)); //@formatter:on
	}

	@ParameterizedTest
	@MethodSource("integer")
	public void readInt(byte[] data, int prefix, int expected) throws InterruptedException, IOException {
		Buffers b = new Buffers();
		b.write(data);

		try (InputStream in = new BuffersInputStream(b)) {
			int value = Http2Headers.readInt(in, b.read(false), prefix);
			assertEquals(expected, value);
		}
	}

	@ParameterizedTest
	@MethodSource("integer")
	public void writeInt(byte[] expected, int prefix, int value) throws InterruptedException {
		Buffers b = new Buffers();
		Http2Headers.writeInt(b, 0, prefix, value);

		assertEquals(expected.length, b.length());
		for (int i = 0; i < expected.length; i++)
			assertEquals(expected[i] & 0xFF, b.get(i));
	}

	public static final byte b(int i) {
		return (byte) (i & 0xFF);
	}

	public static Stream<Arguments> readHeaders() {
		Http2Headers requestRaw = new Http2Headers(4096);
		Http2Headers responseRaw = new Http2Headers(256);
		Http2Headers requestHm = new Http2Headers(4096);
		Http2Headers responseHm = new Http2Headers(256);
		//@formatter:off
		return Stream.of(
				Arguments.of( // literal with indexing
						new Http2Headers(4096),
						new byte[] { b(0x40),b(0x0a),b(0x63),b(0x75),b(0x73),b(0x74),b(0x6f),b(0x6d),b(0x2d),b(0x6b),b(0x65),b(0x79),b(0x0d),b(0x63),b(0x75),b(0x73),b(0x74),b(0x6f),b(0x6d),b(0x2d),b(0x68),b(0x65),b(0x61),b(0x64),b(0x65),b(0x72) },
						new Entry[]{ e("custom-key", "custom-header")},
						new Entry[]{ e("custom-key", "custom-header")}
				),
				Arguments.of( // literal without indexing
						new Http2Headers(4096),
						new byte[] { b(0x04),b(0x0c),b(0x2f),b(0x73),b(0x61),b(0x6d),b(0x70),b(0x6c),b(0x65),b(0x2f),b(0x70),b(0x61),b(0x74),b(0x68)},
						new Entry[]{ e(":path", "/sample/path")},
						new Entry[]{}
				),
				Arguments.of( // literal never indexing
						new Http2Headers(4096),
						new byte[] { b(0x10),b(0x08),b(0x70),b(0x61),b(0x73),b(0x73),b(0x77),b(0x6f),b(0x72),b(0x64),b(0x06),b(0x73),b(0x65),b(0x63),b(0x72),b(0x65),b(0x74)}, 
						new Entry[]{ e("password", "secret")},
						new Entry[]{}),
				Arguments.of( // indexed field
						new Http2Headers(4096),
						new byte[] { b(0x82)}, 
						new Entry[]{ e(":method", "GET")},
						new Entry[]{}),
				
				// request chain without huffman coding
				Arguments.of(
						requestRaw,
						new byte[] { b(0x82),b(0x86),b(0x84),b(0x41),b(0x0f),b(0x77),b(0x77),b(0x77),b(0x2e),b(0x65),b(0x78),b(0x61),b(0x6d),b(0x70),b(0x6c),b(0x65),b(0x2e),b(0x63),b(0x6f),b(0x6d)}, 
						new Entry[]{ e(":method", "GET"), e(":scheme", "http"), e(":path", "/"), e(":authority", "www.example.com")},
						new Entry[]{ e(":authority", "www.example.com")}),
				Arguments.of(
						requestRaw,
						new byte[] { b(0x82),b(0x86),b(0x84),b(0xbe),b(0x58),b(0x08),b(0x6e),b(0x6f),b(0x2d),b(0x63),b(0x61),b(0x63),b(0x68),b(0x65)}, 
						new Entry[]{ e(":method", "GET"), e(":scheme", "http"), e(":path", "/"), e(":authority", "www.example.com"),e("cache-control", "no-cache")},
						new Entry[]{ e("cache-control", "no-cache"), e(":authority", "www.example.com")}),
				Arguments.of(
						requestRaw,
						new byte[] { b(0x82),b(0x87),b(0x85),b(0xbf),b(0x40),b(0x0a),b(0x63),b(0x75),b(0x73),b(0x74),b(0x6f),b(0x6d),b(0x2d),b(0x6b),b(0x65),b(0x79),b(0x0c),b(0x63),b(0x75),b(0x73),b(0x74),b(0x6f),b(0x6d),b(0x2d),b(0x76),b(0x61),b(0x6c),b(0x75),b(0x65)}, 
						new Entry[]{ e(":method", "GET"), e(":scheme", "https"), e(":path", "/index.html"), e(":authority", "www.example.com"),e("custom-key", "custom-value")},
						new Entry[]{ e("custom-key", "custom-value"),e("cache-control", "no-cache"),e(":authority", "www.example.com")}),
				
				// request chain with huffman coding
				Arguments.of(
						requestHm,
						new byte[] { b(0x82),b(0x86),b(0x84),b(0x41),b(0x8c),b(0xf1),b(0xe3),b(0xc2),b(0xe5),b(0xf2),b(0x3a),b(0x6b),b(0xa0),b(0xab),b(0x90),b(0xf4),b(0xff)}, 
						new Entry[]{ e(":method", "GET"), e(":scheme", "http"), e(":path", "/"), e(":authority", "www.example.com")},
						new Entry[]{ e(":authority", "www.example.com")}),
				Arguments.of(
						requestHm,
						new byte[] { b(0x82),b(0x86),b(0x84),b(0xbe),b(0x58),b(0x86),b(0xa8),b(0xeb),b(0x10),b(0x64),b(0x9c),b(0xbf)}, 
						new Entry[]{ e(":method", "GET"), e(":scheme", "http"), e(":path", "/"), e(":authority", "www.example.com"),e("cache-control", "no-cache")},
						new Entry[]{ e("cache-control", "no-cache"), e(":authority", "www.example.com")}),
				Arguments.of(
						requestHm,
						new byte[] { b(0x82),b(0x87),b(0x85),b(0xbf),b(0x40),b(0x88),b(0x25),b(0xa8),b(0x49),b(0xe9),b(0x5b),b(0xa9),b(0x7d),b(0x7f),b(0x89),b(0x25),b(0xa8),b(0x49),b(0xe9),b(0x5b),b(0xb8),b(0xe8),b(0xb4),b(0xbf)}, 
						new Entry[]{ e(":method", "GET"), e(":scheme", "https"), e(":path", "/index.html"), e(":authority", "www.example.com"),e("custom-key", "custom-value")},
						new Entry[]{ e("custom-key", "custom-value"),e("cache-control", "no-cache"),e(":authority", "www.example.com")}),
				
				// response chain without huffman coding
				Arguments.of(
						responseRaw,
						new byte[] { b(0x48),b(0x03),b(0x33),b(0x30),b(0x32),b(0x58),b(0x07),b(0x70),b(0x72),b(0x69),b(0x76),b(0x61),b(0x74),b(0x65),b(0x61),b(0x1d),b(0x4d),b(0x6f),b(0x6e),b(0x2c),b(0x20),b(0x32),b(0x31),b(0x20),b(0x4f),b(0x63),b(0x74),b(0x20),b(0x32),b(0x30),b(0x31),b(0x33),b(0x20),b(0x32),b(0x30),b(0x3a),b(0x31),b(0x33),b(0x3a),b(0x32),b(0x31),b(0x20),b(0x47),b(0x4d),b(0x54),b(0x6e),b(0x17),b(0x68),b(0x74),b(0x74),b(0x70),b(0x73),b(0x3a),b(0x2f),b(0x2f),b(0x77),b(0x77),b(0x77),b(0x2e),b(0x65),b(0x78),b(0x61),b(0x6d),b(0x70),b(0x6c),b(0x65),b(0x2e),b(0x63),b(0x6f),b(0x6d)}, 
						new Entry[]{ e(":status", "302"), e("cache-control", "private"), e("date", "Mon, 21 Oct 2013 20:13:21 GMT"), e("location", "https://www.example.com")},
						new Entry[]{ e("location", "https://www.example.com"), e("date", "Mon, 21 Oct 2013 20:13:21 GMT"), e("cache-control", "private"),e(":status", "302")}),
				Arguments.of(
						responseRaw,
						new byte[] { b(0x48),b(0x03),b(0x33),b(0x30),b(0x37),b(0xc1),b(0xc0),b(0xbf)}, 
						new Entry[]{ e(":status", "307"), e("cache-control", "private"), e("date", "Mon, 21 Oct 2013 20:13:21 GMT"), e("location", "https://www.example.com")},
						new Entry[]{ e(":status", "307"), e("location", "https://www.example.com"), e("date", "Mon, 21 Oct 2013 20:13:21 GMT"), e("cache-control", "private")}),
				Arguments.of(
						responseRaw,
						new byte[] { b(0x88),b(0xc1),b(0x61),b(0x1d),b(0x4d),b(0x6f),b(0x6e),b(0x2c),b(0x20),b(0x32),b(0x31),b(0x20),b(0x4f),b(0x63),b(0x74),b(0x20),b(0x32),b(0x30),b(0x31),b(0x33),b(0x20),b(0x32),b(0x30),b(0x3a),b(0x31),b(0x33),b(0x3a),b(0x32),b(0x32),b(0x20),b(0x47),b(0x4d),b(0x54),b(0xc0),b(0x5a),b(0x04),b(0x67),b(0x7a),b(0x69),b(0x70),b(0x77),b(0x38),b(0x66),b(0x6f),b(0x6f),b(0x3d),b(0x41),b(0x53),b(0x44),b(0x4a),b(0x4b),b(0x48),b(0x51),b(0x4b),b(0x42),b(0x5a),b(0x58),b(0x4f),b(0x51),b(0x57),b(0x45),b(0x4f),b(0x50),b(0x49),b(0x55),b(0x41),b(0x58),b(0x51),b(0x57),b(0x45),b(0x4f),b(0x49),b(0x55),b(0x3b),b(0x20),b(0x6d),b(0x61),b(0x78),b(0x2d),b(0x61),b(0x67),b(0x65),b(0x3d),b(0x33),b(0x36),b(0x30),b(0x30),b(0x3b),b(0x20),b(0x76),b(0x65),b(0x72),b(0x73),b(0x69),b(0x6f),b(0x6e),b(0x3d),b(0x31)}, 
						new Entry[]{ e(":status", "200"), e("cache-control", "private"), e("date", "Mon, 21 Oct 2013 20:13:22 GMT"), e("location", "https://www.example.com"), e("content-encoding", "gzip"), e("set-cookie", "foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1")},
						new Entry[]{ e("set-cookie", "foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1"),  e("content-encoding", "gzip"), e("date", "Mon, 21 Oct 2013 20:13:22 GMT")}),
				
				// response chain with huffman coding
				Arguments.of(
						responseHm,
						new byte[] { b(0x48),b(0x82),b(0x64),b(0x02),b(0x58),b(0x85),b(0xae),b(0xc3),b(0x77),b(0x1a),b(0x4b),b(0x61),b(0x96),b(0xd0),b(0x7a),b(0xbe),b(0x94),b(0x10),b(0x54),b(0xd4),b(0x44),b(0xa8),b(0x20),b(0x05),b(0x95),b(0x04),b(0x0b),b(0x81),b(0x66),b(0xe0),b(0x82),b(0xa6),b(0x2d),b(0x1b),b(0xff),b(0x6e),b(0x91),b(0x9d),b(0x29),b(0xad),b(0x17),b(0x18),b(0x63),b(0xc7),b(0x8f),b(0x0b),b(0x97),b(0xc8),b(0xe9),b(0xae),b(0x82),b(0xae),b(0x43),b(0xd3)}, 
						new Entry[]{ e(":status", "302"), e("cache-control", "private"), e("date", "Mon, 21 Oct 2013 20:13:21 GMT"), e("location", "https://www.example.com")},
						new Entry[]{ e("location", "https://www.example.com"), e("date", "Mon, 21 Oct 2013 20:13:21 GMT"), e("cache-control", "private"),e(":status", "302")}),
				Arguments.of(
						responseHm,
						new byte[] { b(0x48),b(0x83),b(0x64),b(0x0e),b(0xff),b(0xc1),b(0xc0),b(0xbf)}, 
						new Entry[]{ e(":status", "307"), e("cache-control", "private"), e("date", "Mon, 21 Oct 2013 20:13:21 GMT"), e("location", "https://www.example.com")},
						new Entry[]{ e(":status", "307"), e("location", "https://www.example.com"), e("date", "Mon, 21 Oct 2013 20:13:21 GMT"), e("cache-control", "private")}),
				Arguments.of(
						responseHm,
						new byte[] { b(0x88),b(0xc1),b(0x61),b(0x1d),b(0x4d),b(0x6f),b(0x6e),b(0x2c),b(0x20),b(0x32),b(0x31),b(0x20),b(0x4f),b(0x63),b(0x74),b(0x20),b(0x32),b(0x30),b(0x31),b(0x33),b(0x20),b(0x32),b(0x30),b(0x3a),b(0x31),b(0x33),b(0x3a),b(0x32),b(0x32),b(0x20),b(0x47),b(0x4d),b(0x54),b(0xc0),b(0x5a),b(0x04),b(0x67),b(0x7a),b(0x69),b(0x70),b(0x77),b(0x38),b(0x66),b(0x6f),b(0x6f),b(0x3d),b(0x41),b(0x53),b(0x44),b(0x4a),b(0x4b),b(0x48),b(0x51),b(0x4b),b(0x42),b(0x5a),b(0x58),b(0x4f),b(0x51),b(0x57),b(0x45),b(0x4f),b(0x50),b(0x49),b(0x55),b(0x41),b(0x58),b(0x51),b(0x57),b(0x45),b(0x4f),b(0x49),b(0x55),b(0x3b),b(0x20),b(0x6d),b(0x61),b(0x78),b(0x2d),b(0x61),b(0x67),b(0x65),b(0x3d),b(0x33),b(0x36),b(0x30),b(0x30),b(0x3b),b(0x20),b(0x76),b(0x65),b(0x72),b(0x73),b(0x69),b(0x6f),b(0x6e),b(0x3d),b(0x31)}, 
						new Entry[]{ e(":status", "200"), e("cache-control", "private"), e("date", "Mon, 21 Oct 2013 20:13:22 GMT"), e("location", "https://www.example.com"), e("content-encoding", "gzip"), e("set-cookie", "foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1")},
						new Entry[]{ e("set-cookie", "foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1"),  e("content-encoding", "gzip"), e("date", "Mon, 21 Oct 2013 20:13:22 GMT")})
				); //@formatter:on
	}

	@ParameterizedTest
	@MethodSource
	public void readHeaders(Http2Headers h, byte[] data, Entry[] headers, Entry[] table) throws InterruptedException, IOException {
		Buffers b = new Buffers();
		b.write(data);

		Map<String, String> r = new HashMap<>();
		try (InputStream in = new BuffersInputStream(b)) {
			while (b.length() > 0)
				h.readHeader(in, r::put);
		}

		assertEquals(table.length, h.dynamic.size());
		int i = 0;
		for (Entry e : h.dynamic)
			assertEquals(table[i++], e);

		assertEquals(headers.length, r.size());
		for (Entry e : headers)
			assertEquals(e.value(), r.get(e.name()));
	}

	private static final Entry e(String n, String v) {
		return new Entry(n, v);
	}
}
