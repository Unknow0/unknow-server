package unknow.server.util.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
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

	@Test
	public void readIntoBuffers2() throws InterruptedException {
		Buffers b = new Buffers();
		for (int i = 0; i < 5000; i++)
			b.write((byte) i);
		b.skip(4000);

		Buffers w = new Buffers();
		b.read(w, 500, false);
		assertBuff(b, 500);
		assertBuff(w, 500);
	}

	@Test
	public void prepend() throws InterruptedException {
		byte[] b = new byte[500];
		for (int i = 0; i < 500; i++)
			b[i] = (byte) i;

		Buffers buf = new Buffers();
		buf.prepend(b, 0, 500);
		assertBuff(buf, 500);

		buf.prepend(b, 0, 500);
		assertBuff(buf, 1000);
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
		Set<Chunk> set = new HashSet<>();
		while (c != null) {
			assertTrue(set.add(c));
			len += c.l;
			c = c.next;
		}
		assertEquals(l, len, "computed length");
	}
}
