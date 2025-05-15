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
				Arguments.of("t€ß𐍄", false, new byte[][] { test }), //
				Arguments.of("t€ß𐍄", true, new byte[][] { test }), //
				Arguments.of("t€ß𐍄", false, new byte[][] { { 0x74 }, e, s, t }), //
				Arguments.of("ß", false, new byte[][] { { (byte) 0xc3 }, { (byte) 0x9f } }), //
				Arguments.of("€", true, new byte[][] { "%E2%82%ac".getBytes() }), //
				Arguments.of("€", true, new byte[][] { "%E2%8".getBytes(), "2%ac".getBytes() }) //
		);

	}

	@ParameterizedTest
	@MethodSource
	public void testOK(String expected, boolean percent, byte[]... chunks) {
		Utf8Decoder d = new Utf8Decoder(percent);
		for (byte[] b : chunks)
			d.append(b, 0, b.length);
		assertEquals(expected, d.done());
	}

	public static Stream<Arguments> testKO() {
		return Stream.of(Arguments.of(true, new byte[][] { "%E".getBytes() }), //
				Arguments.of(true, new byte[][] { { (byte) 0xc3 } }), //
				Arguments.of(true, new byte[][] { { (byte) 0xe2, (byte) 0x82 } }), //
				Arguments.of(true, new byte[][] { { (byte) 0xf0, (byte) 0x90 } }) //
		);
	}

	@ParameterizedTest
	@MethodSource
	public void testKO(boolean percent, byte[]... chunks) {
		assertThrows(IllegalArgumentException.class, () -> testOK(null, percent, chunks));
	}
}
