/**
 * 
 */
package unknow.server.http.data;

import java.util.Iterator;

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
		return data[i++];
	}
}