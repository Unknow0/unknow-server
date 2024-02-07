/**
 * 
 */
package unknow.server.util.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * simple iterator over an array
 * @param <T> the object type
 */
public class ArrayIt<T> implements Iterator<T> {
	private final T[] data;
	private int i;

	/**
	 * new iterator on array
	 * @param data the array
	 */
	public ArrayIt(T[] data) {
		this.data = data;
		this.i = 0;
	}

	@Override
	public boolean hasNext() {
		return i < data.length;
	}

	@Override
	public T next() {
		if (i == data.length)
			throw new NoSuchElementException();
		return data[i++];
	}
}