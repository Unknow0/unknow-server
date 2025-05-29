package unknow.server.servlet.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class Utf8DecoderTest {
	public static Stream<Arguments> testOK() {
		byte[] test = { 0x74, (byte) 0xe2, (byte) 0x82, (byte) 0xac, (byte) 0xc3, (byte) 0x9f, (byte) 0xf0, (byte) 0x90, (byte) 0x8d, (byte) 0x84 };
		byte[] e = { (byte) 0xe2, (byte) 0x82, (byte) 0xac };
		byte[] s = { (byte) 0xc3, (byte) 0x9f };
		byte[] t = { (byte) 0xf0, (byte) 0x90, (byte) 0x8d, (byte) 0x84 };
		System.out.println(new String(test));
		return Stream.of( //
				Arguments.of("t€ß𐍄", new byte[][] { test }), //
				Arguments.of("t€ß𐍄", new byte[][] { { 0x74 }, e, s, t }), //
				Arguments.of("ß", new byte[][] { { (byte) 0xc3 }, { (byte) 0x9f } }) //
		);

	}

	@ParameterizedTest
	@MethodSource
	public void testOK(String expected, byte[]... chunks) {
		Utf8Decoder d = new Utf8Decoder();
		for (byte[] b : chunks)
			d.append(b, 0, b.length);
		assertEquals(expected, d.done());
	}

	public static Stream<Arguments> testKO() {
		return Stream.of(Arguments.of((Object) new byte[][] { { (byte) 0xc3 } }), //
				Arguments.of((Object) new byte[][] { { (byte) 0xe2, (byte) 0x82 } }), //
				Arguments.of((Object) new byte[][] { { (byte) 0xf0, (byte) 0x90 } }) //
		);
	}

	@ParameterizedTest
	@MethodSource
	public void testKO(byte[]... chunks) {
		assertThrows(IllegalArgumentException.class, () -> testOK(null, chunks));
	}
}
