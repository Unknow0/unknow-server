package io.protostuff;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class Utf8DecoderTest {
	public static final Stream<Arguments> test() {
		return Stream.of(Arguments.of("test", "ß»đßłµþæð丽􀀬😅𝐋j"));
	}

	@ParameterizedTest
	@MethodSource
	public void test(String str) {
		Utf8Decoder d = new Utf8Decoder();
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		for (int i = 0; i < bytes.length; i++)
			d.append(bytes[i]);
		assertEquals(str, d.done());
	}
}
