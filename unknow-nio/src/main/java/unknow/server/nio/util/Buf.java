package unknow.server.nio.util;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * and internal buffer. contains a chunk of byte[]
 * 
 * @author unknow
 */
public final class Buf {
	private static final Queue<Buf> idle = new LinkedBlockingQueue<>();

	public byte[] b;
	public int o;
	public int l;
	public Buf next;

	private Buf() {
	}

	public void set(byte[] b, int o, int l) {
		this.b = b;
		this.o = o;
		this.l = l;
	}

	@Override
	public String toString() {
		return new String(b, o, l);
	}

	/**
	 * get or create a Buf
	 * 
	 * @return a buf
	 */
	public static Buf get() {
		Buf poll;
		synchronized (idle) {
			poll = idle.poll();
		}
		if (poll == null)
			poll = new Buf();
		return poll;
	}

	/**
	 * put back a Buf into the pool
	 * 
	 * @param b the buffer to free
	 * @return next buffer
	 */
	public static Buf free(Buf b) {
		Buf n = b.next;
		b.b = null;
		b.next = null;
		synchronized (idle) {
			idle.offer(b);
		}
		return n;
	}
}