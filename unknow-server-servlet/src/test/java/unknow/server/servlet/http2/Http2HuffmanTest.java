package unknow.server.servlet.http2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersUtils;

public class Http2HuffmanTest {
	public static Stream<Arguments> input() {
		//@formatter:off
		return Stream.of(
				Arguments.of("www.example.com", new byte[] { b(0xf1), b(0xe3), b(0xc2), b(0xe5), b(0xf2), b(0x3a), b(0x6b), b(0xa0), b(0xab), b(0x90), b(0xf4), b(0xff) }),
				Arguments.of("no-cache", new byte[]{b(0xa8),b(0xeb),b(0x10),b(0x64),b(0x9c),b(0xbf)}),
				Arguments.of("custom-key", new byte[]{b(0x25),b(0xa8),b(0x49),b(0xe9),b(0x5b),b(0xa9),b(0x7d),b(0x7f)}),
				Arguments.of("custom-value", new byte[]{b(0x25),b(0xa8),b(0x49),b(0xe9),b(0x5b),b(0xb8),b(0xe8),b(0xb4),b(0xbf)})
				); //@formatter:on
	}

	@ParameterizedTest
	@MethodSource("input")
	public void decode(String decoded, byte[] encoded) throws IOException {
		StringBuilder sb = new StringBuilder();
		try (InputStream b = new ByteArrayInputStream(encoded)) {
			Http2Huffman.decode(b, encoded.length, sb);
		}
		assertEquals(decoded, sb.toString());
	}

	@ParameterizedTest
	@MethodSource("input")
	public void encode(String decoded, byte[] encoded) throws InterruptedException {
		byte[] bytes = decoded.getBytes(StandardCharsets.US_ASCII);
		Buffers b = new Buffers();
		Http2Huffman.encode(b, bytes);
		byte[] array = BuffersUtils.toArray(b, 0, -1);
		assertArrayEquals(encoded, array);
	}

	public static final byte b(int i) {
		return (byte) (i & 0xFF);
	}
}
