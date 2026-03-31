package unknow.server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class Utf8DecoderTest {
	static Stream<String> utf8Strings() {
		return Stream.of("", "hello", "éàç", "こんにちは", "😀😃😄😁", "hello é 😀", "𐍈");
	}

	static Stream<byte[]> invalidUtf8() {
		return Stream.of(new byte[] { (byte) 0x80 }, new byte[] { (byte) 0xC0 }, new byte[] { (byte) 0xE0, (byte) 0x80 },
				//new byte[] { (byte) 0xF0, (byte) 0x80, (byte) 0x80 },
				//new byte[] { (byte) 0xC0, (byte) 0xAF },
				// new byte[] { (byte) 0xE0, (byte) 0x9F, (byte) 0x80 },
				new byte[] { (byte) 0xF8 }, new byte[] { (byte) 0xFF });
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testFastPath(String input) {
		Decoder decoder = new Utf8Decoder();
		ByteBuffer bbuf = ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8));
		CharBuffer cbuf = CharBuffer.allocate(100);

		decoder.decode(bbuf, cbuf, true);

		assertEquals(input, cbuf.flip().toString());
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testFastPathChuncked(String input) {
		Decoder decoder = new Utf8Decoder();

		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		CharBuffer cbuf = CharBuffer.allocate(100);

		for (int i = 0; i < bytes.length; i++) {
			ByteBuffer bbuf = ByteBuffer.wrap(new byte[] { bytes[i] });
			decoder.decode(bbuf, cbuf, i == bytes.length - 1);
		}

		assertEquals(input, cbuf.flip().toString());
	}

	@ParameterizedTest
	@MethodSource("invalidUtf8")
	void testFastPathError(byte[] input) {
		Decoder decoder = new Utf8Decoder();

		ByteBuffer bbuf = ByteBuffer.wrap(input);
		CharBuffer cbuf = CharBuffer.allocate(10);

		assertThrows(IllegalArgumentException.class, () -> decoder.decode(bbuf, cbuf, true));
	}

	@ParameterizedTest
	@MethodSource("invalidUtf8")
	void testFastPathErrorChuncked(byte[] input) {
		Decoder decoder = Decoder.from(StandardCharsets.UTF_8);

		CharBuffer cbuf = CharBuffer.allocate(10);

		assertThrows(IllegalArgumentException.class, () -> {
			for (int i = 0; i < input.length; i++) {
				ByteBuffer bbuf = ByteBuffer.wrap(new byte[] { input[i] });
				decoder.decode(bbuf, cbuf, i == input.length - 1);
			}
		});
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testSlowPath(String input) {
		ByteBuffer bbuf = ByteBuffer.wrap(input.getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
		CharBuffer cbuf = CharBuffer.allocate(100);

		new Utf8Decoder().decode(bbuf, cbuf, true);

		assertEquals(input, cbuf.flip().toString());
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testSlowPathChuncked(String input) {
		Decoder decoder = new Utf8Decoder();

		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		CharBuffer cbuf = CharBuffer.allocate(100);

		for (int i = 0; i < bytes.length; i++) {
			ByteBuffer bbuf = ByteBuffer.wrap(new byte[] { bytes[i] }).asReadOnlyBuffer();
			decoder.decode(bbuf, cbuf, i == bytes.length - 1);
		}

		assertEquals(input, cbuf.flip().toString());
	}

	@ParameterizedTest
	@MethodSource("invalidUtf8")
	void testSlowPathError(byte[] input) {
		Decoder decoder = new Utf8Decoder();

		ByteBuffer bbuf = ByteBuffer.wrap(input).asReadOnlyBuffer();
		CharBuffer cbuf = CharBuffer.allocate(10);

		assertThrows(IllegalArgumentException.class, () -> decoder.decode(bbuf, cbuf, true));
	}

	@ParameterizedTest
	@MethodSource("invalidUtf8")
	void testSlowPathErrorChuncked(byte[] input) {
		Decoder decoder = Decoder.from(StandardCharsets.UTF_8);

		CharBuffer cbuf = CharBuffer.allocate(10);

		assertThrows(IllegalArgumentException.class, () -> {
			for (int i = 0; i < input.length; i++) {
				ByteBuffer bbuf = ByteBuffer.wrap(new byte[] { input[i] }).asReadOnlyBuffer();
				decoder.decode(bbuf, cbuf, i == input.length - 1);
			}
		});
	}
}
