/**
 * 
 */
package unknow.server.nio.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;

/**
 * a chain of buffer
 * 
 * @author unknow
 */
public class Buffers {
	private Buf head;
	private Buf tail;
	private int size;

	/**
	 * append a byte
	 * 
	 * @param b
	 */
	public synchronized void append(byte b) {
		if (tail != null && tail.l < tail.b.length - 1) {
			tail.b[tail.l++] = b;
			return;
		}
		Buf buf = Buf.get();
		buf.set(new byte[255], 0, 1);
		buf.b[0] = b;
		append(buf);
	}

	/**
	 * append data to this buffers
	 * 
	 * @param buffer the data to add
	 */
	public void append(ByteBuffer buffer) {
		int l = buffer.remaining();
		byte[] b = new byte[l];
		buffer.get(b);
		append(b, 0, l);
	}

	/**
	 * append data to this buffers. the data isn't copied
	 * 
	 * @param b the data
	 * @param o offset
	 * @param l length
	 */

	public void append(byte[] b, int o, int l) {
		Buf buf = Buf.get();
		buf.set(b, o, l);
		append(buf);
	}

	/**
	 * append a buf in this chain
	 * 
	 * @param b
	 */
	private synchronized void append(Buf b) {
		if (tail != null)
			tail.next = b;
		tail = b;
		if (head == null)
			head = b;
		size += b.l;
	}

	/**
	 * add data in front of this buffers
	 * 
	 * @param buffer data to get
	 */
	public void prepend(ByteBuffer buffer) {
		int l = buffer.remaining();
		byte[] b = new byte[l];
		buffer.get(b);
		prepend(b, 0, l);
	}

	/**
	 * add data in front of this buffers
	 * 
	 * @param b data
	 * @param o offset
	 * @param l length
	 */
	public synchronized void prepend(byte[] b, int o, int l) {
		Buf buf = Buf.get();
		buf.set(b, o, l);
		buf.next = head;
		head = buf;
	}

	/**
	 * discard bytes
	 * 
	 * @param l number of byte to discard
	 */
	public synchronized void skip(int l) {
		do {
			if (head.l > l) {
				head.o += l;
				head.l -= l;
				size -= l;
				return;
			}
			l -= head.l;
			size -= head.l;
			head = Buf.free(head);
		} while (head != null && l > 0);
	}

	/**
	 * read data into a ByteBuffer
	 * 
	 * @param bb where to read data
	 * @return true if we read something
	 */
	public synchronized boolean read(ByteBuffer bb) {
		if (head == null || !bb.hasRemaining())
			return false;
		int l = bb.remaining();
		do {
			if (head.l > l) {
				bb.put(head.b, head.o, l);
				head.o += l;
				head.l -= l;
				size -= l;
				return true;
			}
			bb.put(head.b, head.o, head.l);
			l -= head.l;
			size -= head.l;
			head = Buf.free(head);
		} while (head != null && l > 0);
		return true;
	}

	/**
	 * @return the byte or -1 if beffers is empty
	 */
	public synchronized int read() {
		if (head == null)
			return -1;
		byte b = head.b[head.o];
		size--;
		if (--head.l == 0)
			head = Buf.free(head);
		else
			head.o++;
		return b;
	}

	/**
	 * read data into another buffers
	 * 
	 * @param l   number of byte to move
	 * @param out the output
	 */
	public synchronized void read(int l, Buffers out) {
		do {
			if (head.l > l) {
				Buf buf = Buf.get();
				buf.set(head.b, head.o, l);
				out.append(buf);
				head.o += l;
				head.l -= l;
				size -= l;
				break;
			}
			l -= head.l;
			size -= head.l;
			Buf n = head;
			head = head.next;
			n.next = null;
			out.append(n);
		} while (head != null && l > 0);
	}

	/**
	 * read data into byte array
	 * 
	 * @parma buf output data
	 * @param o offset
	 * @param l number of byte to move
	 * @return actual number of byte written
	 */
	public synchronized int read(byte[] buf, int o, int l) {
		do {
			if (head.l > l) {
				System.arraycopy(head.b, head.o, buf, o, l);
				head.o += l;
				head.l -= l;
				size -= l;
				return o + l;
			}
			System.arraycopy(head.b, head.o, buf, o, head.l);
			l -= head.l;
			o += head.l;
			size -= head.l;
			head = Buf.free(head);
		} while (head != null && l > 0);
		return o;
	}

	/**
	 * @return the number of bytes in this buffers
	 */
	public int size() {
		return size;
	}

	/**
	 * check if this buffers contains some byte
	 * 
	 * @param lookup byte to lookup
	 * @param limit  check only the "limit" first bytes
	 * @return -1 if the data is not found, else the index of the first lookup
	 */
	public int indexOf(byte lookup, int limit) {
		return indexOf(lookup, 0, limit);
	}

	/**
	 * check if this buffers contains some byte
	 * 
	 * @param lookup byte to lookup
	 * @param start  position of the first byte to check
	 * @param limit  check only the "limit" first bytes
	 * @return -1 if the data is not found, else the index of the first lookup
	 */
	public int indexOf(byte lookup, int start, int limit) {
		if (head == null)
			return -1;
		if (limit < 0)
			limit = size - start;
		int i = 0;
		Buf b = head;
		do {
			int e = b.o + b.l;
			if (b.l < start) {
				start -= b.l;
				continue;
			}
			for (int j = b.o + start; j < e && i < limit; j++, i++) {
				if (b.b[j] == lookup)
					return i;
			}
		} while ((b = b.next) != null);
		return -1;
	}

	/**
	 * check if this buffers contains some bytes
	 * 
	 * @param lookup bytes to lookup
	 * @param limit  check only the first "limit" bytes
	 * @return -1 if the data is not found, else the index of the first lookup
	 */
	public synchronized int indexOf(byte[] lookup, int limit) {
		if (head == null)
			return -1;
		int i = 0, k = 0;
		int l = lookup.length;
		if (limit < 0)
			limit = size;
		else
			limit += l;
		Buf b = head;
		do {
			int e = b.o + b.l;
			for (int j = b.o; j < e && i < limit; j++) {
				i++;
				if (b.b[j] == lookup[k])
					k++;
				else
					k = 0;
				if (k == l)
					return i - l;
			}
		} while ((b = b.next) != null);
		return -1;
	}

	/**
	 * return the head of these buffers
	 * 
	 * @return the head
	 */
	public Buf getHead() {
		return head;
	}

	/**
	 * @return true if this buffers is empty
	 */
	public boolean isEmpty() {
		return head == null;
	}

	/**
	 * remove all data
	 */
	public synchronized void clear() {
		Buf b = head;
		tail = head = null;
		size = 0;
		while (b != null)
			b = Buf.free(b);
	}

	/**
	 * @param bytes byte array to fill
	 * @return all this buffers as byte array
	 */
	public synchronized byte[] toBytes(byte[] bytes) {
		if (bytes.length < size)
			bytes = new byte[size];
		Buf b = head;
		int i = 0;
		do {
			System.arraycopy(b.b, b.o, bytes, i, b.l);
			i += b.l;
			b = b.next;
		} while (b != null);
		return bytes;
	}

	@Override
	public String toString() {
		if (head == null)
			return "";
		StringBuilder sb = new StringBuilder();
		Buf b = head;
		do {
			sb.append(b);
		} while ((b = b.next) != null);
		return sb.toString();
	}
}
