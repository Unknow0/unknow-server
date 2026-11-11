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
public class ArrayIt<T> extends AbstractArrayIt<T> implements ListIterator<T> {
	private final T[] data;

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
		super(o, l, i);
		this.data = data;
	}

	@Override
	public T next() {
		if (i == l)
			throw new NoSuchElementException();
		return data[o + i++];
	}

	@Override
	public T previous() {
		if (i == 0)
			throw new NoSuchElementException();
		return data[--i + o];
	}
}