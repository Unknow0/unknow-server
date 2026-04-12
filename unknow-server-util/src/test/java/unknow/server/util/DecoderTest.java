package unknow.server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DecoderTest {
	static Stream<String> utf8Strings() {
		return Stream.of("", "hello", "éàç", "こんにちは", "😀😃😄😁", "hello é 😀", "𐍈");
	}

	static Stream<byte[]> invalidUtf8() {
		return Stream.of(new byte[] { (byte) 0x80 }, new byte[] { (byte) 0xC0 }, new byte[] { (byte) 0xE0, (byte) 0x80 }, new byte[] { (byte) 0xF0, (byte) 0x80, (byte) 0x80 },
				new byte[] { (byte) 0xC0, (byte) 0xAF }, new byte[] { (byte) 0xE0, (byte) 0x9F, (byte) 0x80 }, new byte[] { (byte) 0xF8 }, new byte[] { (byte) 0xFF });
	}

	protected Decoder decoder() {
		return new Decoder.DefaultDecoder(StandardCharsets.UTF_8.newDecoder());
	}

	protected void decodeAll(Decoder decoder, ByteBuffer bbuf, CharBuffer cbuf) {
		decoder.decode(bbuf, cbuf, true);
		while (decoder.flush(cbuf))
			;
	}

	protected void decodeChunck(Decoder decoder, ByteBuffer input, CharBuffer cbuf) {
		int l = input.limit();
		for (int i = 0; i < l; i++) {
			ByteBuffer bbuf = input.slice(i, 1);
			decoder.decode(bbuf, cbuf, i == l - 1);
		}
		while (decoder.flush(cbuf))
			;
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testFastPath(String input) {
		Decoder decoder = decoder();
		ByteBuffer bbuf = ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8));
		CharBuffer cbuf = CharBuffer.allocate(100);
		decodeAll(decoder, bbuf, cbuf);

		assertEquals(input, cbuf.flip().toString());
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testFastPathChuncked(String input) {
		Decoder decoder = decoder();

		ByteBuffer bytes = ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8));
		CharBuffer cbuf = CharBuffer.allocate(100);
		decodeChunck(decoder, bytes, cbuf);
		assertEquals(input, cbuf.flip().toString());
	}

	@ParameterizedTest
	@MethodSource("invalidUtf8")
	void testFastPathError(byte[] input) {
		Decoder decoder = decoder();

		ByteBuffer bbuf = ByteBuffer.wrap(input);
		CharBuffer cbuf = CharBuffer.allocate(100);

		assertThrows(IllegalArgumentException.class, () -> decodeAll(decoder, bbuf, cbuf));
	}

	@ParameterizedTest
	@MethodSource("invalidUtf8")
	void testFastPathErrorChuncked(byte[] bytes) {
		Decoder decoder = decoder();

		ByteBuffer bbuf = ByteBuffer.wrap(bytes);
		CharBuffer cbuf = CharBuffer.allocate(100);

		assertThrows(IllegalArgumentException.class, () -> decodeChunck(decoder, bbuf, cbuf));
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testSlowPath(String input) {
		Decoder decoder = decoder();

		ByteBuffer bbuf = ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
		CharBuffer cbuf = CharBuffer.allocate(100);

		decodeAll(decoder, bbuf, cbuf);

		assertEquals(input, cbuf.flip().toString());
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testSlowPathChuncked(String input) {
		Decoder decoder = decoder();

		ByteBuffer bytes = ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
		CharBuffer cbuf = CharBuffer.allocate(100);

		decodeChunck(decoder, bytes, cbuf);

		assertEquals(input, cbuf.flip().toString());
	}

	@ParameterizedTest
	@MethodSource("invalidUtf8")
	void testSlowPathError(byte[] input) {
		Decoder decoder = decoder();

		ByteBuffer bbuf = ByteBuffer.wrap(input).asReadOnlyBuffer();
		CharBuffer cbuf = CharBuffer.allocate(100);

		assertThrows(IllegalArgumentException.class, () -> decodeAll(decoder, bbuf, cbuf));
	}

	@ParameterizedTest
	@MethodSource("invalidUtf8")
	void testSlowPathErrorChuncked(byte[] bytes) {
		Decoder decoder = decoder();

		ByteBuffer bbuf = ByteBuffer.wrap(bytes).asReadOnlyBuffer();
		CharBuffer cbuf = CharBuffer.allocate(100);

		assertThrows(IllegalArgumentException.class, () -> decodeChunck(decoder, bbuf, cbuf));
	}
}
