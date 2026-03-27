package unknow.server.util.io;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ByteBufferCache {
	private static final Map<Integer, ByteBufferCache> CACHES = new HashMap<>();

	private final int size;
	private final int max;
	private final ConcurrentLinkedQueue<ByteBuffer> idle;
	private final AtomicInteger count;

	private ByteBufferCache(int size, int max) {
		this.size = size;
		this.max = max;
		this.idle = new ConcurrentLinkedQueue<>();
		this.count = new AtomicInteger(0);
	}

	public static void createCache(int size, int max) {
		CACHES.computeIfAbsent(size, k -> new ByteBufferCache(size, max));
	}

	public static ByteBufferCached get(int size) {
		ByteBufferCache b = CACHES.get(size);
		if (b != null)
			return b.get();
		return new ByteBufferCached(null, ByteBuffer.allocate(size));
	}

	public ByteBufferCached get() {
		ByteBuffer poll = idle.poll();
		if (poll == null)
			poll = ByteBuffer.allocate(size);
		return new ByteBufferCached(this, poll);
	}

	private void free(ByteBuffer b) {
		b.clear();
		if (count.getAndUpdate(x -> x == max ? x : x + 1) < max)
			idle.offer(b);
	}

	public static class ByteBufferCached {
		private final ByteBufferCache cache;
		private ByteBuffer buf;

		private ByteBufferCached(ByteBufferCache cache, ByteBuffer buf) {
			this.cache = cache;
			this.buf = buf;
		}

		public ByteBuffer buffer() {
			return buf;
		}

		public void free() {
			if (cache != null)
				cache.free(buf);
			buf = null;
		}
	}
}
