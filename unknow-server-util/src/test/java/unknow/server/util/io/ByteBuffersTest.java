package unknow.server.util.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ByteBuffersTest {
	@Test
	void tracksAndDrainsBuffers() {
		ByteBuffers buffers = new ByteBuffers(1);
		ByteBuffer first = ByteBuffer.allocate(4);
		first.put(new byte[] { 1, 2 });
		ByteBuffer second = ByteBuffer.allocate(4);
		second.put(new byte[] { 3, 4, 5 });

		buffers.accept(first);
		buffers.accept(second);
		assertEquals(3, buffers.remaining());

		List<ByteBuffer> drained = new ArrayList<>();
		buffers.drain(b -> drained.add(b));
		assertEquals(2, drained.size());
		assertEquals(0, buffers.remaining());
		assertEquals(2, drained.get(0).remaining());
		assertEquals(3, drained.get(1).remaining());
		assertEquals(1, drained.get(0).get());
		assertEquals(3, drained.get(1).get());
	}

	@Test
	void compactsEmptyBuffersAtTheFront() {
		ByteBuffers buffers = new ByteBuffers(2);
		ByteBuffer empty = ByteBuffer.allocate(1);
		empty.put((byte) 1).flip();
		empty.get();
		ByteBuffer data = ByteBuffer.wrap(new byte[] { 2 });
		buffers.accept(empty);
		buffers.accept(data);

		buffers.compact();

		assertEquals(1, buffers.len);
		assertEquals(1, buffers.remaining());
		assertTrue(buffers.buf[0].hasRemaining());
	}
}
