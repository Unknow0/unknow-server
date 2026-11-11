package unknow.server.util.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class LimitedInputStreamTest {
	@Test
	void singleByteReadsStopAtLimit() throws IOException {
		try (LimitedInputStream in = new LimitedInputStream(
				new ByteArrayInputStream("abcdef".getBytes(StandardCharsets.UTF_8)))) {
			in.setLimit(3);

			assertEquals('a', in.read());
			assertEquals('b', in.read());
			assertEquals('c', in.read());
			assertEquals(-1, in.read());
		}
	}

	@Test
	void bulkReadsStopAtLimitAndThenReturnEof() throws IOException {
		try (LimitedInputStream in = new LimitedInputStream(
				new ByteArrayInputStream("abcdef".getBytes(StandardCharsets.UTF_8)))) {
			in.setLimit(3);
			byte[] out = new byte[8];

			assertEquals(3, in.read(out, 0, out.length));
			assertEquals("abc", new String(out, 0, 3, StandardCharsets.UTF_8));
			assertEquals(-1, in.read(out, 0, out.length));
		}
	}

	@Test
	void skipCannotPassTheLimit() throws IOException {
		try (LimitedInputStream in = new LimitedInputStream(
				new ByteArrayInputStream("abcdef".getBytes(StandardCharsets.UTF_8)))) {
			in.setLimit(2);

			assertEquals(2, in.skip(5));
			assertEquals(-1, in.read());
		}
	}

	@Test
	void negativeSkipReturnsZero() throws IOException {
		try (LimitedInputStream in = new LimitedInputStream(
				new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)))) {
			in.setLimit(3);

			assertEquals(0, in.skip(-1));
			assertEquals('a', in.read());
		}
	}
}