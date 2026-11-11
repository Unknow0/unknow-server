package unknow.server.util.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ByteBufferReaderTest {
	@Test
	void decodesUtf8SplitAcrossBuffers() throws IOException {
		ByteBufferInputStream in = new ByteBufferInputStream();
		byte[] bytes = "A😀éZ".getBytes(StandardCharsets.UTF_8);
		in.addBuffer(ByteBuffer.wrap(bytes, 0, 3));
		in.addBuffer(ByteBuffer.wrap(bytes, 3, 2));
		in.addBuffer(ByteBuffer.wrap(bytes, 5, bytes.length - 5));
		in.close();

		try (ByteBufferReader reader = new ByteBufferReader(in, StandardCharsets.UTF_8)) {
			char[] result = new char[16];
			int length = 0;
			int read;
			while ((read = reader.read(result, length, result.length - length)) != -1)
				length += read;

			assertEquals("A😀éZ", new String(result, 0, length));
		}
	}

	@Test
	void flushesNonUtf8DecoderAtEof() throws IOException {
		ByteBufferInputStream in = new ByteBufferInputStream();
		byte[] bytes = "終端".getBytes(StandardCharsets.UTF_16LE);
		in.addBuffer(ByteBuffer.wrap(bytes));
		in.close();

		try (ByteBufferReader reader = new ByteBufferReader(in, StandardCharsets.UTF_16LE)) {
			char[] result = new char[8];
			int length = 0;
			int read;
			while ((read = reader.read(result, length, result.length - length)) != -1)
				length += read;

			assertEquals("終端", new String(result, 0, length));
		}
	}

	@Test
	void zeroCapacityCharBufferReadReturnsZero() throws IOException {
		ByteBufferInputStream in = new ByteBufferInputStream();
		in.close();
		try (ByteBufferReader reader = new ByteBufferReader(in, StandardCharsets.UTF_8)) {
			assertEquals(0, reader.read(CharBuffer.allocate(0)));
		}
	}
}
