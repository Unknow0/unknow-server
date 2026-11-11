package unknow.server.util.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class ByteBufferInputStreamTest {
	@Test
	void readsAcrossBuffersAndReturnsUnsignedBytes() throws IOException {
		ByteBufferInputStream in = new ByteBufferInputStream();
		in.addBuffer(ByteBuffer.wrap(new byte[] { (byte) 0xFF, 'a' }));
		in.addBuffer(ByteBuffer.wrap(new byte[] { 'b' }));
		in.close();

		assertEquals(255, in.read());
		assertEquals('a', in.read());
		assertEquals('b', in.read());
		assertEquals(-1, in.read());
	}

	@Test
	void skipAdvancesFromCurrentPositionAndAcrossBuffers() throws IOException {
		ByteBufferInputStream in = new ByteBufferInputStream();
		in.addBuffer(ByteBuffer.wrap("012345".getBytes()).position(2));
		in.addBuffer(ByteBuffer.wrap("abc".getBytes()));
		in.close();

		assertEquals(5, in.skip(5));
		assertEquals('b', in.read());
	}

	@Test
	void zeroLengthReadReturnsZeroEvenAtEof() throws IOException {
		ByteBufferInputStream in = new ByteBufferInputStream();
		in.close();

		assertEquals(0, in.read(new byte[0], 0, 0));
	}

	@Test
	void rejectsInvalidReadBounds() {
		try (ByteBufferInputStream in = new ByteBufferInputStream()) {
			assertThrows(IndexOutOfBoundsException.class, () -> in.read(new byte[2], 1, 2));
		}
	}
}
