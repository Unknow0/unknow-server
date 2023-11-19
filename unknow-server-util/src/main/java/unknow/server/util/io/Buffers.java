/**
 * 
 */
package unknow.server.util.io;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import unknow.server.util.pool.Pool;
import unknow.server.util.pool.SharedPool;

/**
 * @author unknow
 */
public class Buffers {
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();

	private Chunk head;
	private Chunk tail;
	private int len;

	/**
	 * 
	 * @return the current number of bytes
	 */
	public int length() {
		lock.lock();
		try {
			return len;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * append one byte to the buffer
	 * 
	 * @param b byte to add
	 * @throws InterruptedException
	 */
	public void write(int b) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (tail == null)
				head = tail = Chunk.get();
			if (tail.o + tail.l == 4096) {
				tail.next = Chunk.get();
				tail = tail.next;
			}
			tail.b[tail.o + tail.l] = (byte) b;
			tail.l++;
			len++;
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * append data into this buffers, same as @{code write(buf, 0, buf.length)}
	 * 
	 * @param buf the data to add
	 * @throws InterruptedException
	 */
	public void write(byte[] buf) throws InterruptedException {
		write(buf, 0, buf.length);
	}

	/**
	 * append data into this buffers
	 * 
	 * @param buf the data to add
	 * @param o   the offset
	 * @param l   the length of data to write
	 * @throws InterruptedException
	 */
	public void write(byte[] buf, int o, int l) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (l == 0)
				return;
			if (tail == null)
				head = tail = Chunk.get();
			len += l;
			tail = writeInto(tail, buf, o, l);
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * append data to this buffers
	 * 
	 * @param bb data to append
	 */
	public void write(ByteBuffer bb) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (bb.remaining() == 0)
				return;
			if (tail == null)
				head = tail = Chunk.get();
			len += bb.remaining();
			tail = writeInto(tail, bb);
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * add data in front of this buffers
	 * 
	 * @param buf data to add
	 * @param o   offset
	 * @param l   length
	 * @throws InterruptedException
	 */
	public void prepend(byte[] buf, int o, int l) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			len += l;
			if (head != null && l < head.o) {
				System.arraycopy(buf, o, head.b, head.o - l, l);
				return;
			}
			Chunk c = Chunk.get();
			head = c;
			c = writeInto(c, buf, o, l);
			c.next = head;
			if (tail == null)
				tail = c;
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * add data in front of this buffers
	 * 
	 * @param buf data to append
	 * @throws InterruptedException
	 */
	public void prepend(ByteBuffer buf) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			int r = buf.remaining();
			len += r;
			if (head != null && r < head.o) {
				buf.get(head.b, head.o - r, r);
				return;
			}
			Chunk c = Chunk.get();
			head = c;
			c = writeInto(c, buf);
			c.next = head;
			if (tail == null)
				tail = c;
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * read one byte
	 * 
	 * @param wait if true wait for data
	 * @return -1 if no more byte are readable
	 * @throws InterruptedException
	 */
	public int read(boolean wait) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (wait)
				awaitContent();
			if (len == 0)
				return -1;
			int r = head.b[head.o++];
			if (--len == 0)
				tail = null;
			if (--head.l == 0)
				head = Chunk.free(head);
			return r;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * read data from this buffers
	 * 
	 * @param buf  where to read
	 * @param o    offset
	 * @param l    max length to read
	 * @param wait if true wait for data
	 * @return the number of bytes read
	 * @throws InterruptedException
	 */
	public int read(byte[] buf, int o, int l, boolean wait) throws InterruptedException {
		if (l == 0)
			return 0;
		lock.lockInterruptibly();
		try {
			if (wait)
				awaitContent();
			if (len == 0)
				return -1;
			int v = 0;
			while (head != null && v < l) {
				int r = Math.min(l - v, head.l);
				System.arraycopy(head.b, head.o, buf, o, r);
				len -= r;
				v += r;
				o += r;
				if (r == head.l) {
					head = Chunk.free(head);
					if (head == null)
						tail = null;
				} else {
					head.o += r;
					head.l -= r;
				}
			}
			return v;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * read data from this buffers
	 * 
	 * @param bb   where to read
	 * @param wait if true wait for data
	 * @return true if data where read
	 * @throws InterruptedException
	 */
	public boolean read(ByteBuffer bb, boolean wait) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (wait)
				awaitContent();
			int l = bb.remaining();
			if (head == null || l == 0)
				return false;
			while (head != null && l > 0) {
				int r = Math.min(l, head.l);
				bb.put(head.b, head.o, r);
				len -= r;
				l -= r;
				if (r == head.l) {
					head = Chunk.free(head);
					if (head == null)
						tail = null;
				} else {
					head.o += r;
					head.l -= r;
				}
			}
			return true;
		} finally {
			lock.unlock();
		}
	}

	private void awaitContent() throws InterruptedException {
		while (len == 0)
			cond.await();
	}

	/**
	 * read a number amount of byte into the buffers
	 * 
	 * @param buf  where to read
	 * @param wait if true wait for data
	 * @param l    number of byte to read
	 * @throws InterruptedException
	 */
	public void read(Buffers buf, int l, boolean wait) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (wait)
				awaitContent();
			if (l == 0 || head == null)
				return;
			Chunk h = head;
			Chunk last = null;
			int read = 0;

			Chunk c = head;
			// read whole chunk
			while (c != null && l >= c.l) {
				l -= c.l;
				read += c.l;
				last = c;
				c = c.next;
			}

			if (c != null && l > 0) {
				Chunk n = Chunk.get();
				System.arraycopy(c.b, c.o, n.b, 0, l);
				c.o += l;
				c.l -= l;
				n.l = l;
				read += l;
				if (last != null)
					last.next = n;
				else
					h = n;
				last = n;
			} else { // we move all
				if (last != null)
					last.next = null;
			}
			buf.lock.lockInterruptibly();
			try {
				len -= read;
				head = c;
				if (c == null)
					tail = null;

				buf.len += read;
				if (buf.head == null)
					buf.head = h;
				else
					buf.tail.next = h;
				buf.tail = last;
			} finally {
				buf.lock.unlock();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * skip l bytes
	 * 
	 * @param l number of byte to skip
	 * @throws InterruptedException
	 */
	public void skip(int l) throws InterruptedException {
		if (l < 0)
			throw new IllegalArgumentException("length < 0");
		if (l == 0)
			return;
		lock.lockInterruptibly();
		try {
			while (head != null && l >= head.l) {
				l -= head.l;
				len -= head.l;
				head = Chunk.free(head);
			}
			if (head != null) {
				head.o += l;
				head.l -= l;
				len -= l;
			} else
				tail = null;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * clear this buffers
	 * 
	 */
	public void clear() {
		lock.lock();
		try {
			len = 0;
			Chunk c = head;
			while (c != null)
				c = Chunk.free(c);
			head = tail = null;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @return true if this buffers is empty
	 */
	public boolean isEmpty() {
		lock.lock();
		try {
			return len == 0;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * iterate over the chunk
	 * 
	 * @param w walker
	 * @param o start index
	 * @param l max length to process
	 * @return false if Walker returned false
	 * @throws InterruptedException
	 */
	public WalkResult walk(Walker w, int o, int l) throws InterruptedException {
		if (l == 0)
			return WalkResult.MAX;
		lock.lockInterruptibly();
		try {
			if (len == 0 || o >= len)
				return WalkResult.END;
			if (l < 0)
				l = len;
			Chunk c = head;
			while (c.l < o) {
				o -= c.l;
				c = c.next;
				if (c == null)
					return WalkResult.END;
			}
			int i = Math.min(l, c.l - o);
			if (!w.apply(c.b, c.o + o, c.o + o + i))
				return WalkResult.STOPED;
			l -= i;
			while (l > 0 && (c = c.next) != null) {
				i = Math.min(l, c.l);
				if (!w.apply(c.b, c.o, c.o + i))
					return WalkResult.STOPED;
				l -= i;
			}
			return l == 0 ? WalkResult.MAX : WalkResult.END;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * get a single byte
	 * 
	 * @param off offset
	 * @return the byte or -1 if off outside of buffers range
	 * @throws InterruptedException
	 */
	public int get(int off) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			Chunk b = head;
			if (b == null || off > len)
				return -1;
			while (b.l <= off) {
				off -= b.l;
				b = b.next;
				if (b == null)
					return -1;
			}
			return b.b[b.o + off];
		} finally {
			lock.unlock();
		}
	}

	/**
	 * write data in the chunk
	 * 
	 * @param c  where to append
	 * @param bb data to append
	 * @return end chunk of added data
	 */
	private final Chunk writeInto(Chunk c, ByteBuffer bb) {
		int l = bb.remaining();
		for (;;) {
			int r = Math.min(l, 4096 - c.l - c.o);
			bb.get(c.b, c.o + c.l, r);
			c.l += r;
			l -= r;
			if (l == 0)
				return c;
			c.next = Chunk.get();
			c = c.next;
		}
	}

	/**
	 * write data in the chunk
	 * 
	 * @param c where to append
	 * @param b data to append
	 * @param o offset
	 * @param l length
	 * @return end chunk of added data
	 */
	private final Chunk writeInto(Chunk c, byte[] buf, int o, int l) {
		for (;;) {
			int r = Math.min(l, 4096 - c.l - c.o);
			System.arraycopy(buf, o, c.b, c.o + c.l, r);
			c.l += r;
			l -= r;
			o += r;
			if (l == 0)
				return c;
			c.next = Chunk.get();
			c = c.next;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			BuffersUtils.toString(sb, this, 0, len);
		} catch (@SuppressWarnings("unused") InterruptedException e) { // ignore
			Thread.currentThread().interrupt();
		}
		return sb.toString();
	}

	public interface Walker {
		/**
		 * apply a bloc of data
		 * 
		 * @param b data
		 * @param o start index
		 * @param e end index
		 * @return false to stop the walk
		 */
		boolean apply(byte[] b, int o, int e);
	}

	public static enum WalkResult {
		END, STOPED, MAX
	}

	/**
	 * a chunk of data
	 * 
	 * @author unknow
	 */
	public static class Chunk {
		private static final Pool<Chunk> POOL = new SharedPool<>(1000, pool -> new Chunk());
		/** the content */
		public final byte[] b;
		/** the current offset */
		public int o;
		/** the current length */
		public int l;
		/** next chunk of data of null if in end */
		public Chunk next;

		private Chunk() {
			b = new byte[4096];
			o = l = 0;
		}

		/**
		 * create or get an idle chunk
		 * 
		 * @return a chunk
		 */
		public static Chunk get() {
			return POOL.get();
		}

		/**
		 * free a chunk
		 * 
		 * @param c the chunk to free
		 * @return the next of the freed chunk
		 */
		public static Chunk free(Chunk c) {
			Chunk n = c.next;
			c.o = c.l = 0;
			c.next = null;
			POOL.free(c);
			return n;
		}
	}
}