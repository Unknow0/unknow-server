package unknow.server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class Utf8EncoderTest {
	static Stream<Arguments> utf8Strings() {
		return Stream.of(Arguments.of("", ""), Arguments.of("hello", "hello"), Arguments.of("éàç", "éàç"), Arguments.of("こんにちは", "こんにちは"),
				Arguments.of("😀😃😄😁", "😀😃😄😁"), Arguments.of("hello é 😀", "hello é 😀"), Arguments.of("𐍈", "𐍈"), Arguments.of("\uD800", "\uFFFD"),
				Arguments.of("\uDC00", "\uFFFD"), Arguments.of("\uD800A", "\uFFFDA"), Arguments.of("A\uDC00", "A\uFFFD"));
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testFastPath(String input, String expected) {
		Encoder decoder = new Utf8Encoder();
		CharBuffer cbuf = CharBuffer.wrap(input.toCharArray());
		ByteBuffer bbuf = ByteBuffer.allocate(100);

		decoder.encode(cbuf, bbuf, true);

		assertEquals(expected, new String(bbuf.array(), 0, bbuf.position(), StandardCharsets.UTF_8));
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testFastPathChuncked(String input, String expected) {
		Encoder decoder = new Utf8Encoder();
		char[] chars = input.toCharArray();
		ByteBuffer bbuf = ByteBuffer.allocate(100);

		for (int i = 0; i < chars.length; i++) {
			CharBuffer cbuf = CharBuffer.wrap(new char[] { chars[i] });
			decoder.encode(cbuf, bbuf, i == chars.length - 1);
		}

		assertEquals(expected, new String(bbuf.array(), 0, bbuf.position(), StandardCharsets.UTF_8));
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testSlowPath(String input, String expected) {
		Encoder encoder = new Utf8Encoder();
		CharBuffer cbuf = CharBuffer.wrap(input.toCharArray());
		ByteBuffer bbuf = ByteBuffer.allocate(100);

		encoder.encode(cbuf, bbuf, true);

		assertEquals(expected, new String(bbuf.array(), 0, bbuf.position(), StandardCharsets.UTF_8));
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testSlowPathChuncked(String input, String expected) {
		Encoder decoder = new Utf8Encoder();
		char[] chars = input.toCharArray();
		ByteBuffer bbuf = ByteBuffer.allocate(100);

		for (int i = 0; i < chars.length; i++) {
			CharBuffer cbuf = CharBuffer.wrap(new char[] { chars[i] }).asReadOnlyBuffer();
			decoder.encode(cbuf, bbuf, i == chars.length - 1);
		}

		assertEquals(expected, new String(bbuf.array(), 0, bbuf.position(), StandardCharsets.UTF_8));
	}

}
