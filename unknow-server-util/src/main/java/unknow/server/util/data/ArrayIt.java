/**
 * 
 */
package unknow.server.util.data;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * simple iterator over an array
 * @param <T> the object type
 */
public class ArrayIt<T> implements ListIterator<T> {
	private final T[] data;
	private final int o;
	private final int l;
	private int i;

	/**
	 * new iterator on array
	 * @param data the array
	 */
	public ArrayIt(T[] data) {
		this(data, 0, data.length, 0);
	}

	/**
	 * new iterator on array
	 * @param data the array
	 * @param o start array index
	 * @param l array length length
	 * @param i start index
	 */
	public ArrayIt(T[] data, int o, int l, int i) {
		this.data = data;
		this.o = o;
		this.l = l;
		this.i = i;
	}

	@Override
	public boolean hasNext() {
		return i < l;
	}

	@Override
	public T next() {
		if (i == l)
			throw new NoSuchElementException();
		return data[o + i++];
	}

	@Override
	public boolean hasPrevious() {
		return i > 0;
	}

	@Override
	public T previous() {
		if (i == 0)
			throw new NoSuchElementException();
		return data[--i + o];
	}

	@Override
	public int nextIndex() {
		return i - o;
	}

	@Override
	public int previousIndex() {
		return i - 1 - o;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(T e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(T e) {
		throw new UnsupportedOperationException();
	}
}