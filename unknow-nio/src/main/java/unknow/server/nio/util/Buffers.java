/**
 * 
 */
package unknow.server.nio.util;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author unknow
 */
public class Buffers {
	private Chunk head;
	private Chunk tail;
	private int len;

	public Chunk getHead() {
		return head;
	}

	/**
	 * 
	 * @return the current number of bytes
	 */
	public int length() {
		return len;
	}

	/**
	 * @param b
	 */
	public synchronized void write(int b) {
		if (tail == null)
			head = tail = Chunk.get();
		if (tail.o + tail.l == 4096) {
			tail.next = Chunk.get();
			tail = tail.next;
		}
		tail.b[tail.o + tail.l] = (byte) b;
		tail.l++;
		len++;
		this.notifyAll();
	}

	/**
	 * append data into this buffers
	 * 
	 * @param buf the data to add
	 * @param o   the offset
	 * @param l   the length of data to write
	 */
	public synchronized void write(byte[] buf, int o, int l) {
		if (l == 0)
			return;
		if (tail == null)
			head = tail = Chunk.get();
		len += l;
		writeInto(tail, buf, o, l);
		this.notifyAll();
	}

	/**
	 * append data to this buffers
	 * 
	 * @param bb data to append
	 */
	public synchronized void write(ByteBuffer bb) {
		if (bb.remaining() == 0)
			return;
		if (tail == null)
			head = tail = Chunk.get();
		len += bb.remaining();
		writeInto(tail, bb);
		this.notifyAll();
	}

	public synchronized void prepend(byte b) {
		len++;
		if (head != null && head.o > 0) {
			head.o--;
			head.b[head.o] = b;
			return;
		}
		Chunk c = Chunk.get();
		c.b[0] = b;
		c.l = 1;
		c.next = head;
		head = c;
		if (tail == null)
			tail = head;
	}

	/**
	 * add data in front of this buffers
	 * 
	 * @param buf data to add
	 * @param o   offset
	 * @param l   length
	 */
	public synchronized void prepend(byte[] buf, int o, int l) {
		len += l;
		if (head != null && l < head.o) {
			System.arraycopy(buf, o, head.b, head.o - l, l);
			return;
		}
		Chunk c = Chunk.get();
		c.next = head;
		head = c;
		c = writeInto(c, buf, o, l);
		if (tail == null)
			tail = c;
	}

	/**
	 * add data in front of this buffers
	 * 
	 * @param buf data to append
	 */
	public synchronized void prepend(ByteBuffer buf) {
		int r = buf.remaining();
		len += r;
		if (head != null && r < head.o) {
			buf.get(head.b, head.o - r, r);
			return;
		}
		Chunk c = Chunk.get();
		c.next = head;
		head = c;
		c = writeInto(c, buf);
		if (tail == null)
			tail = c;
	}

	/**
	 * read one byte
	 * 
	 * @return -1 if no more byte are readable
	 */
	public synchronized int read() {
		if (len == 0)
			return -1;
		int r = head.b[head.o++];
		if (len-- == 0)
			tail = null;
		if (--head.l == 0)
			head = Chunk.free(head);
		return r;
	}

	/**
	 * read data from this buffers
	 * 
	 * @param buf where to read
	 * @param o   offset
	 * @param l   max length to read
	 * @return the number of bytes read
	 */
	public synchronized int read(byte[] buf, int o, int l) {
		if (head == null || l == 0)
			return -1;
		int v = 0;
		while (head != null && v < l) {
			int r = Math.min(l - v, head.l);
			System.arraycopy(head.b, head.o, buf, o, r);
			len -= r;
			v += r;
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
	}

	/**
	 * read data from this buffers
	 * 
	 * @param bb where to read
	 * @return true if data where read
	 */
	public synchronized boolean read(ByteBuffer bb) {
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
	}

	/**
	 * read a number amount of byte into the buffers
	 * 
	 * @param buf where to read
	 * @param l   number of byte to read
	 */
	public synchronized void read(Buffers buf, int l) {
		if (l == 0 || head == null)
			return;
		Chunk h = head;
		Chunk c = head;
		Chunk last = null;
		int read = 0;
		while (c != null && l > c.l) {
			l -= c.l;
			read += c.l;
			last = c;
			c = c.next;
		}
		if (c == null)
			;
		else if (l > 0) {
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
		} else
			last.next = null;

		len -= read;
		head = c;
		if (c == null)
			tail = c;
		synchronized (buf) {
			buf.len += read;
			if (buf.head == null)
				buf.head = buf.tail = h;
			else
				buf.tail.next = h;
		}
	}

	/**
	 * skip l bytes
	 * 
	 * @param l number of byte to skip
	 */
	public synchronized void skip(int l) {
		while (head != null && l > head.l) {
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
	}

	/**
	 * clear this buffers
	 */
	public synchronized void clear() {
		len = 0;
		Chunk c = head;
		while (c != null)
			c = Chunk.free(c);
		head = tail = null;
	}

	/**
	 * @return true if this buffers is empty
	 */
	public boolean isEmpty() {
		return len == 0;
	}

	/**
	 * find a byte in the buffers
	 * 
	 * @param b   the byte to find
	 * @param max the maximum number of byte to read
	 * @return -1 if not found, -2 if max reached
	 */
	public int indexOf(byte b, int max) {
		return indexOf(b, 0, max);
	}

	/**
	 * find a byte in the buffers
	 * 
	 * @param b   the byte to find
	 * @param off start offset
	 * @param len the maximum number of byte to read
	 * @return -1 if not found, -2 if max reached
	 */
	public synchronized int indexOf(byte b, int off, int len) {
		if (this.len - off < 1)
			return -1;
		if (len < 0)
			len = this.len + 1;
		Chunk c = head;
		int l;
		int o = off;
		while (c.l < off) {
			off -= c.l;
			c = c.next;
			if (c == null)
				return -1;
		}
		if (off > 0) {
			l = Math.min(c.l, len);
			for (int i = 0; i < l; i++) {
				if (c.b[i + off + c.o] == b)
					return o + i;
			}
			len -= l;
			o += l;
			if (len == 0)
				return -2;
			c = c.next;
			if (c == null)
				return -1;
		}
		for (;;) {
			l = Math.min(c.l, len);
			for (int i = 0; i < l; i++) {
				if (c.b[i + off + c.o] == b)
					return o + i;
			}
			len -= l;
			o += l;
			if (len == 0)
				return -2;
			c = c.next;
			if (c == null)
				return -1;
		}
	}

	/**
	 * find a bytes in the buffers
	 * 
	 * @param b   the byte to find
	 * @param max the maximum number of byte to read
	 * @return -1 if not found, -2 if max reached
	 */
	public int indexOf(byte[] b, int max) {
		return indexOf(b, 0, max);
	}

	public synchronized int indexOf(byte[] b, int o, int max) {
		if (len - o < b.length)
			return -1;
		Chunk c = head;
		int l = o;
		int j = 0;
		while (c != null && c.l < o) {
			o -= c.l;
			c = c.next;
		}
		while (c != null) {
			loop: for (int i = c.o + o; i < c.o + c.l; i++) {
				for (int k = i; j < b.length; j++, k++) {
					if (c.b[k] != b[j]) {
						j = 0;
						continue loop;
					}
				}
				if (j == b.length)
					return i - c.o;
			}
			o = 0;
			l += c.l;
			if (max >= 0 && l > max)
				return -2;
			c = c.next;
		}
		return -1;
	}

	/**
	 * get index of next byte that is one of
	 * 
	 * @param b   byte to look for
	 * @param off first index
	 * @param len max length
	 * @return index of or -1 if not found, -2 if len reached
	 */
	public synchronized int indexOfOne(byte[] b, int off, int len) {
		if (this.len - off < 1)
			return -1;
		if (len < 0)
			len = this.len + 1;
		Chunk c = head;
		int l;
		int o = off;
		while (c.l < off) {
			off -= c.l;
			c = c.next;
			if (c == null)
				return -1;
		}
		if (off > 0) {
			l = Math.min(c.l, len);
			for (int i = 0; i < l; i++) {
				int k = i + off + c.o;
				for (int j = 0; j < b.length; j++) {
					if (c.b[k] == b[j])
						return o + i;
				}
			}
			len -= l;
			o += l;
			if (len == 0)
				return -2;
			c = c.next;
			if (c == null)
				return -1;
		}
		for (;;) {
			l = Math.min(c.l, len);
			for (int i = 0; i < l; i++) {
				int k = i + off + c.o;
				for (int j = 0; j < b.length; j++) {
					if (c.b[k] == b[j])
						return o + i;
				}
			}
			len -= l;
			o += l;
			if (len == 0)
				return -2;
			c = c.next;
			if (c == null)
				return -1;
		}
	}

	/**
	 * get index of next byte that isn't one of
	 * 
	 * @param b   byte to not look for
	 * @param off first index
	 * @param len max length
	 * @return index of or -1 if not found, -2 if len reached
	 */
	public synchronized int indexOfNot(byte[] b, int off, int len) {
		if (this.len - off < 1)
			return -1;
		if (len < 0)
			len = this.len + 1;
		Chunk c = head;
		int l;
		int o = off;
		while (c.l < off) {
			off -= c.l;
			c = c.next;
			if (c == null)
				return -1;
		}
		if (off > 0) {
			l = Math.min(c.l, len);
			loop: for (int i = 0; i < l; i++) {
				int k = i + off + c.o;
				for (int j = 0; j < b.length; j++) {
					if (c.b[k] == b[j])
						continue loop;
				}
				return o + i;
			}
			len -= l;
			o += l;
			if (len == 0)
				return -2;
			c = c.next;
			if (c == null)
				return -1;
		}
		for (;;) {
			l = Math.min(c.l, len);
			loop: for (int i = 0; i < l; i++) {
				int k = i + off + c.o;
				for (int j = 0; j < b.length; j++) {
					if (c.b[k] == b[j])
						continue loop;
				}
				return o + i;
			}
			len -= l;
			o += l;
			if (len == 0)
				return -2;
			c = c.next;
			if (c == null)
				return -1;
		}
	}

	/**
	 * get a single byte
	 * 
	 * @param off offset
	 * @return the byte or -1 if off outside of buffers range
	 */
	public synchronized int get(int off) {
		Chunk b = head;
		if (b == null || off > len)
			return -1;
		while (b.l < off) {
			off -= b.l;
			b = b.next;
			if (b == null)
				return -1;
		}
		return b.b[b.o + off];
	}

	/**
	 * @param p
	 * @param off
	 * @param len
	 */
	public synchronized void get(byte[] p, int off, int len) {
		Chunk b = head;
		if (b == null)
			return;
		if (len == 0 || off >= length())
			return;
		if (len == -1)
			len = length() - off;
		int o = 0;
		do {
			if (b.l < off) {
				off -= b.l;
				continue;
			}
			int w = b.l - off;
			System.arraycopy(b.b, b.o, p, o, w);
			off = 0;
			len -= w;
			o += w;
		} while (len > 0 && (b = b.next) != null);
	}

	/**
	 * @param part
	 * @param o
	 * @return true if the part equals
	 */
	public synchronized boolean equals(byte[] part, int o) {
		if (len - o < part.length)
			return false;
		Chunk c = head;
		while (c != null && c.l < o) {
			o -= c.l;
			c = c.next;
		}
		int j = 0;
		while (c != null) {
			for (int i = c.o + o; i < c.o + c.l; i++) {
				if (c.b[i] != part[j++])
					return false;
				if (j == part.length)
					return true;
			}
			c = c.next;
		}
		return false;
	}

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
		toString(sb, 0, len);
		return sb.toString();
	}

	/**
	 * convert the buffers to string assuming ascii encoding
	 * 
	 * @param sb
	 * @param off
	 * @param len
	 */
	public synchronized void toString(StringBuilder sb, int off, int len) {
		Chunk b = head;
		if (b == null || len == 0 || off >= this.len)
			return;
		if (len == -1 || len > this.len - off)
			len = this.len - off;
		sb.ensureCapacity(len);
		char[] c = new char[4096];

		while (b.l < off) {
			off -= b.l;
			if ((b = b.next) == null)
				return;
		}
		if (off > 0) {
			int e = Math.min(b.o + len, b.l);
			for (int i = b.o; i < e; i++)
				c[i - b.o] = (char) b.b[i + off];
			int l = e - b.o;
			sb.append(c, 0, l);
			len -= l;
			b = b.next;
		}
		while (len > 0 && b != null) {
			int e = b.o + (b.l < len ? b.l : len);
			for (int i = b.o; i < e; i++)
				c[i - b.o] = (char) b.b[i];
			int l = e - b.o;
			sb.append(c, 0, l);
			len -= l;
			b = b.next;
		}
	}

	/**
	 * a chunk of data
	 * 
	 * @author unknow
	 */
	public static class Chunk {
		private static final Queue<Chunk> POOL = new LinkedBlockingQueue<>();
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
		}

		/**
		 * create or get an idle chunk
		 * 
		 * @return a chunk
		 */
		public static Chunk get() {
			Chunk poll;
			synchronized (POOL) {
				poll = POOL.poll();
			}
			if (poll == null)
				poll = new Chunk();
			return poll;
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
			POOL.offer(c);
			return n;
		}
	}
}