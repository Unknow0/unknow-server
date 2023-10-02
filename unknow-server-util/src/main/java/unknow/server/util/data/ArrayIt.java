/**
 * 
 */
package unknow.server.util.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayIt<T> implements Iterator<T> {
	private final T[] data;
	private int i;

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
		if(i==data.length)
			throw new NoSuchElementException();
		return data[i++];
	}
}