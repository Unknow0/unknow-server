/**
 * 
 */
package unknow.server.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author unknow
 */
public class Buffers {
	public static final int BUF_LEN = 4096;
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();

	Chunk head;
	Chunk tail;
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
	 * @throws InterruptedException on interrupt
	 */
	public void write(int b) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (tail == null)
				head = tail = new Chunk();
			if (tail.o + tail.l == BUF_LEN) {
				tail.next = new Chunk();
				tail = tail.next;
			}
			tail.b[tail.o + tail.l] = (byte) (b & 0xFF);
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
	 * @throws InterruptedException on interrupt
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
	 * @throws InterruptedException on interrupt
	 */
	public void write(byte[] buf, int o, int l) throws InterruptedException {
		if (l == 0)
			return;
		lock.lockInterruptibly();
		try {
			if (tail == null)
				head = tail = new Chunk();
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
	 * @throws InterruptedException on interrupt
	 */
	public void write(ByteBuffer bb) throws InterruptedException {
		if (bb.remaining() == 0)
			return;
		lock.lockInterruptibly();
		try {
			if (tail == null)
				head = tail = new Chunk();
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
	 * @throws InterruptedException on interrupt
	 */
	public void prepend(byte[] buf, int o, int l) throws InterruptedException {
		if (o < 0 || l < 0 || o + l > buf.length)
			throw new IndexOutOfBoundsException("o: " + o + ", l: " + l + ", buf.length: " + buf.length);
		if (l == 0)
			return;
		lock.lockInterruptibly();
		try {
			len += l;
			if (head != null && l < head.o) {
				head.o -= l;
				head.l += l;
				System.arraycopy(buf, o, head.b, head.o, l);
				return;
			}
			Chunk c = new Chunk();
			Chunk t = writeInto(c, buf, o, l);
			if (tail == null)
				tail = t;
			else
				t.next = head;
			head = c;
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * add data in front of this buffers
	 * 
	 * @param buf data to append
	 * @throws InterruptedException on interrupt
	 */
	public void prepend(ByteBuffer buf) throws InterruptedException {
		if (buf.remaining() == 0)
			return;
		lock.lockInterruptibly();
		try {
			int l = buf.remaining();
			len += l;
			if (head != null && l < head.o) {
				head.o -= l;
				head.l += l;
				buf.get(head.b, head.o, l);
				return;
			}
			Chunk c = new Chunk();
			Chunk t = writeInto(c, buf);
			if (tail == null)
				tail = t;
			else
				t.next = head;
			head = c;
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	/**
	* add data in front of this buffers
	* 
	* @param buf data to append
	* @throws InterruptedException on interrupt
	*/
	public void prepend(Buffers buf) throws InterruptedException {
		lock.lockInterruptibly();
		buf.lock.lockInterruptibly();
		try {
			if (buf.isEmpty())
				return;
			len += buf.len;
			buf.tail.next = head;
			head = buf.head;

			buf.len = 0;
			buf.head = buf.tail = null;
			cond.signalAll();
		} finally {
			buf.lock.unlock();
			lock.unlock();
		}
	}

	/**
	 * read one byte
	 * 
	 * @param wait if true wait for data
	 * @return -1 if no more byte are readable
	 * @throws InterruptedException on interrupt
	 */
	public int read(boolean wait) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (wait)
				awaitContent();
			if (len == 0)
				return -1;
			int r = head.b[head.o++] & 0xFF;
			if (--len == 0)
				tail = null;
			if (--head.l == 0)
				head = head.next;
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
	 * @throws InterruptedException on interrupt
	 */
	public int read(byte[] buf, int o, int l, boolean wait) throws InterruptedException {
		if (o < 0 || l < 0 || o + l > buf.length)
			throw new IndexOutOfBoundsException("o: " + o + ", l: " + l + ", buf.length: " + buf.length);
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
					head = head.next;
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
	 * @throws InterruptedException on interrupt
	 */
	public boolean read(ByteBuffer bb, boolean wait) throws InterruptedException {
		if (bb.remaining() == 0)
			return false;
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
					head = head.next;
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

	/**
	 * wait for more content to be written
	 * @throws InterruptedException
	 */
	public void awaitContent() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			while (len == 0)
				cond.await();
		} finally {
			lock.unlock();
		}
	}

	public void lock() throws InterruptedException {
		lock.lockInterruptibly();
	}

	public void unlock() {
		lock.unlock();
	}

	/**
	 * wait for the signal (data added or signal is called)
	 */
	public void await() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			cond.await();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * wake up all thread in await()
	 */
	public void signal() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * append chunk
	 * @param c chunk to add
	 * @throws InterruptedException on interrupt
	 */
	private void append(Chunk c) throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (tail == null)
				head = c;
			else
				tail.next = c;
			len += c.l;
			while (c.next != null) {
				c = c.next;
				len += c.l;
			}
			tail = c;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * read a number amount of byte into the buffers
	 * 
	 * @param buf  where to read
	 * @param l    number of byte to read
	 * @param wait if true wait for data
	 * @throws InterruptedException on interrupt
	 */
	public void read(Buffers buf, int l, boolean wait) throws InterruptedException {
		if (l == 0)
			return;
		lock.lockInterruptibly();
		try {
			if (wait)
				awaitContent();
			if (len == 0 || head == null)
				return;
			if (l == -1)
				l = len;

			if (l >= len) { // move all content
				buf.append(head);
				head = tail = null;
				len = 0;
				return;
			}

			Chunk h = head;
			Chunk last = null;
			Chunk c = head;
			// read whole chunk
			while (l >= c.l) {
				l -= c.l;
				len -= c.l;
				last = c;
				c = c.next;
			}

			if (last == null || l > 0) {
				Chunk n = new Chunk();
				System.arraycopy(c.b, c.o, n.b, 0, l);
				c.o += l;
				c.l -= l;
				n.l = l;
				len -= l;
				if (last != null)
					last.next = n;
				else
					h = n;
				last = n;
			} else
				last.next = null;

			buf.append(h);

			head = c;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * read a number amount of byte into the buffers
	 * 
	 * @param out  where to read
	 * @param l    number of byte to read
	 * @param wait if true wait for data
	 * @throws InterruptedException on interrupt
	 * @throws IOException 
	 */
	public void read(OutputStream out, int l, boolean wait) throws InterruptedException, IOException {
		if (l == 0)
			return;
		lock.lockInterruptibly();
		try {
			if (wait)
				awaitContent();
			if (len == 0 || head == null)
				return;
			if (l == -1)
				l = len;

			Chunk last = null;
			Chunk c = head;
			// read whole chunk
			while (c != null && l >= c.l) {
				out.write(c.b, c.o, c.l);
				l -= c.l;
				len -= c.l;
				last = c;
				c = c.next;
			}
			if (last != null)
				last.next = null;

			if (c != null && l > 0) {
				out.write(c.b, c.o, l);
				c.o += l;
				c.l -= l;
				len -= l;
				head = c;
			}
			head = c;
			if (c == null)
				tail = null;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * skip l bytes
	 * 
	 * @param l number of byte to skip
	 * @return actual number of byte skiped
	 * @throws InterruptedException on interrupt
	 */
	public long skip(long l) throws InterruptedException {
		if (l < 0)
			throw new IllegalArgumentException("length < 0");
		if (l == 0)
			return 0;
		long s = 0;
		lock.lockInterruptibly();
		try {
			while (head != null && l >= head.l) {
				l -= head.l;
				len -= head.l;
				s += head.l;
				head = head.next;
			}
			if (head != null) {
				head.o += l;
				head.l -= l;
				len -= l;
				s += l;
			} else
				tail = null;
			return s;
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
	 * @throws InterruptedException on interrupt
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
			}
			int i = Math.min(l, c.l - o);
			if (!w.apply(c.b, c.o + o, c.o + o + i))
				return WalkResult.STOPED;
			l -= i;
			while ((c = c.next) != null && l > c.l) {
				if (!w.apply(c.b, c.o, c.o + c.l))
					return WalkResult.STOPED;
				l -= c.l;
			}
			if (c == null || (c = c.next) == null)
				return WalkResult.END;

			if (l > 0) {
				if (!w.apply(c.b, c.o, c.o + l))
					return WalkResult.STOPED;
			}
			return WalkResult.MAX;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * get a single byte
	 * 
	 * @param off offset
	 * @return the byte or -1 if off outside of buffers range
	 * @throws InterruptedException on interrupt
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
			return b.b[b.o + off] & 0xFF;
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
	private static final Chunk writeInto(Chunk c, ByteBuffer bb) {
		int l = bb.remaining();
		for (;;) {
			int r = Math.min(l, BUF_LEN - c.l - c.o);
			bb.get(c.b, c.o + c.l, r);
			c.l += r;
			l -= r;
			if (l == 0)
				return c;
			c = c.next = new Chunk();
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
	private static final Chunk writeInto(Chunk c, byte[] buf, int o, int l) {
		for (;;) {
			int r = Math.min(l, BUF_LEN - c.l - c.o);
			System.arraycopy(buf, o, c.b, c.o + c.l, r);
			c.l += r;
			l -= r;
			o += r;
			if (l == 0)
				return c;
			c = c.next = new Chunk();
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

	/** interface to walk over a buffers chain */
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

	/** walk result */
	public static enum WalkResult {
		/** walk reached the end of the buffers */
		END,
		/** walk stopped */
		STOPED,
		/** walk reached the limit */
		MAX
	}

	/**
	 * a chunk of data
	 * 
	 * @author unknow
	 */
	public static class Chunk {
		/** the content */
		public final byte[] b;
		/** the current offset */
		public int o;
		/** the current length */
		public int l;
		/** next chunk of data of null if in end */
		public Chunk next;

		private Chunk() {
			b = new byte[BUF_LEN];
			o = l = 0;
		}
	}

}