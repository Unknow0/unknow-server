package unknow.server.util.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import unknow.server.util.io.Buffers.Chunk;

public class BuffersTest {
	public static Stream<Arguments> write() {
		return Stream.of(Arguments.of(500), Arguments.of(4096), Arguments.of(5000));
	}

	@ParameterizedTest
	@MethodSource
	public void write(int l) throws InterruptedException {
		Buffers b = new Buffers();

		for (int i = 0; i < l; i++)
			b.write((byte) i);
		assertBuff(b, l);
	}

	public static Stream<Arguments> readIntoBuffers() {
		return Stream.of(Arguments.of(500, 100), Arguments.of(500, 500), Arguments.of(4096, 100), Arguments.of(4096, 5000), Arguments.of(5000, 100), Arguments.of(5000, 4500));
	}

	@ParameterizedTest
	@MethodSource
	public void readIntoBuffers(int l, int r) throws InterruptedException {
		Buffers b = new Buffers();
		for (int i = 0; i < l; i++)
			b.write((byte) i);

		Buffers w = new Buffers();
		b.read(w, r, false);
		assertBuff(b, Math.max(0, l - r));
		assertBuff(w, Math.min(l, r));
	}

	private void assertBuff(Buffers b, int l) {
		assertEquals(l, b.length(), "length");

		if (l == 0) {
			assertNull(b.head);
			assertNull(b.tail);
			return;
		}

		int len = 0;
		Chunk c = b.head;
		while (c != null) {
			len += c.l;
			c = c.next;
		}
		assertEquals(l, len, "computed length");
	}
}
