/**
 * 
 */
package unknow.server.util.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author unknow
 */
public class ArraySet<T extends Comparable<T>> implements Set<T> {
	private static final Comparable<?>[] EMPTY = new Comparable[0];
	private final T[] data;

	public ArraySet(T[] data) {
		this(Arrays.asList(data));
	}

	@SuppressWarnings("unchecked")
	public ArraySet(Collection<T> data) {
		if (!(data instanceof Set))
			data = new HashSet<>(data);
		this.data = (T[]) data.toArray(EMPTY);
		Arrays.sort(this.data);
	}

	@Override
	public int size() {
		return data.length;
	}

	@Override
	public boolean isEmpty() {
		return data.length == 0;
	}

	@Override
	public boolean contains(Object o) {
		return Arrays.binarySearch(data, o) >= 0;
	}

	@Override
	public Iterator<T> iterator() {
		return new ArrayIt<>(data);
	}

	@Override
	public Object[] toArray() {
		return Arrays.copyOf(data, data.length);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R[] toArray(R[] a) {
		R[] r = a.length >= data.length ? a : (R[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), data.length);
		System.arraycopy(data, 0, r, 0, data.length);
		if (a.length > data.length)
			r[data.length] = null;
		return r;
	}

	@Override
	public boolean add(T e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
}
