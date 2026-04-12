package unknow.server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EncoderTest {
	static Stream<Arguments> utf8Strings() {
		return Stream.of(Arguments.of("", ""), Arguments.of("hello", "hello"), Arguments.of("éàç", "éàç"), Arguments.of("こんにちは", "こんにちは"),
				Arguments.of("😀😃😄😁", "😀😃😄😁"), Arguments.of("hello é 😀", "hello é 😀"), Arguments.of("𐍈", "𐍈"), Arguments.of("\uD800", "\uFFFD"),
				Arguments.of("\uDC00", "\uFFFD"), Arguments.of("\uD800A", "\uFFFDA"), Arguments.of("A\uDC00", "A\uFFFD"));
	}

	protected Encoder encoder() {
		return new Encoder.DefaultEncoder(StandardCharsets.UTF_8.newEncoder().replaceWith(Encoder.REPL).onMalformedInput(CodingErrorAction.REPLACE));
	}

	private void encodeAll(Encoder encoder, CharBuffer cbuf, ByteBuffer bbuf) {
		encoder.encode(cbuf, bbuf, true);
		while (encoder.flush(bbuf))
			;
	}

	private void encodeChunck(Encoder encoder, String input, ByteBuffer bbuf) {
		char[] chars = input.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			CharBuffer cbuf = CharBuffer.wrap(new char[] { chars[i] });
			encoder.encode(cbuf, bbuf, i == chars.length - 1);
		}
		while (encoder.flush(bbuf))
			;
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testFastPath(String input, String expected) {
		Encoder encoder = encoder();
		CharBuffer cbuf = CharBuffer.wrap(input.toCharArray());
		ByteBuffer bbuf = ByteBuffer.allocate(100);

		encodeAll(encoder, cbuf, bbuf);
		assertEquals(expected, new String(bbuf.array(), 0, bbuf.position(), StandardCharsets.UTF_8));
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testFastPathChuncked(String input, String expected) {
		Encoder encoder = encoder();
		ByteBuffer bbuf = ByteBuffer.allocate(100);
		encodeChunck(encoder, input, bbuf);
		assertEquals(expected, new String(bbuf.array(), 0, bbuf.position(), StandardCharsets.UTF_8));
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testSlowPath(String input, String expected) {
		Encoder encoder = encoder();
		CharBuffer cbuf = CharBuffer.wrap(input.toCharArray());
		ByteBuffer bbuf = ByteBuffer.allocate(100);

		encodeAll(encoder, cbuf, bbuf);

		assertEquals(expected, new String(bbuf.array(), 0, bbuf.position(), StandardCharsets.UTF_8));
	}

	@ParameterizedTest
	@MethodSource("utf8Strings")
	void testSlowPathChuncked(String input, String expected) {
		Encoder encoder = encoder();
		ByteBuffer bbuf = ByteBuffer.allocate(100);

		encodeChunck(encoder, input, bbuf);

		assertEquals(expected, new String(bbuf.array(), 0, bbuf.position(), StandardCharsets.UTF_8));
	}

}
