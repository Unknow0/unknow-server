/**
 * 
 */
package unknow.server.util.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author unknow
 * @param <T> value type
 */
public class IntArrayMap<T> {
	private int[] keys;
	private T[] values;
	private int len;

	/**
	 * create an empty array map
	 */
	@SuppressWarnings("unchecked")
	public IntArrayMap() {
		keys = new int[10];
		values = (T[]) new Object[10];
		len = 0;
	}

	/**
	 * create a new arrayMap with these key/value /!\ key should already be sorted
	 * 
	 * @param key   the keys
	 * @param value the values
	 */
	public IntArrayMap(int[] key, T[] value) {
		if (key.length != value.length)
			throw new IllegalArgumentException("different number of key and value");
		this.keys = key;
		this.values = value;
		this.len = key.length;
	}

	/**
	 * @param key the key
	 * @return true if the key exists
	 */
	public boolean contains(int key) {
		return Arrays.binarySearch(keys, 0, len, key) >= 0;
	}

	/**
	 * @param key the key
	 * @return the associated value
	 */
	public T get(int key) {
		int i = Arrays.binarySearch(keys, 0, len, key);
		return i < 0 ? null : values[i];
	}

	/**
	 * set a value
	 * 
	 * @param key   the key
	 * @param value the value
	 * @return the old value
	 */
	public T set(int key, T value) {
		int i = Arrays.binarySearch(keys, 0, len, key);
		if (i >= 0) {
			T old = values[i];
			values[i] = value;
			return old;
		}
		ensure(++len);
		i = -i - 1;
		if (i < len - 1) {
			System.arraycopy(keys, i, keys, i + 1, len - i - 1);
			System.arraycopy(values, i, values, i + 1, len - i - 1);
		}
		keys[i] = key;
		values[i] = value;
		return null;
	}

	/**
	 * set the key only if missing
	 * 
	 * @param key   the key
	 * @param value the value
	 * @return true if the map changed
	 */
	public boolean setOnce(int key, T value) {
		int i = Arrays.binarySearch(keys, 0, len, key);
		if (i >= 0)
			return false;
		ensure(++len);
		i = -i - 1;
		if (i < len - 1) {
			System.arraycopy(keys, i, keys, i + 1, len - i - 1);
			System.arraycopy(values, i, values, i + 1, len - i - 1);
		}
		keys[i] = key;
		values[i] = value;
		return true;
	}

	/**
	 * @param key the key to remove
	 * @return the removed value
	 */
	public T remove(int key) {
		int i = Arrays.binarySearch(keys, 0, len, key);
		if (i < 0)
			return null;
		T old = values[i];
		len--;
		System.arraycopy(keys, i + 1, keys, i, len - i);
		System.arraycopy(values, i + 1, values, i, len - i);
		values[len] = null;
		return old;
	}

	/**
	 * empty the map
	 */
	public void clear() {
		for (int i = 0; i < len; i++)
			values[i] = null;
		len = 0;
	}

	/**
	 * ensure that the internal array are big enouth
	 * 
	 * @param l minimal size
	 */
	private void ensure(int l) {
		if (l < keys.length)
			return;
		keys = Arrays.copyOf(keys, l);
		values = Arrays.copyOf(values, l);
	}

	/**
	 * @return map size
	 */
	public int size() {
		return len;
	}

	/**
	 * @return true if map is empty
	 */
	public boolean isEmpty() {
		return len == 0;
	}

	/**
	 * @return set of all the keys
	 */
	public Set<Integer> keySet() {
		return new KeySet();
	}

	public Collection<T> values() {
		return new Values();
	}

	private class KeySet implements Set<Integer> {

		@Override
		public int size() {
			return len;
		}

		@Override
		public boolean isEmpty() {
			return len == 0;
		}

		@Override
		public boolean contains(Object o) {
			return IntArrayMap.this.get((Integer) o) != null;
		}

		@Override
		public Iterator<Integer> iterator() {
			return new KeyIt();
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <E> E[] toArray(E[] a) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean add(Integer e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends Integer> c) {
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

	private class KeyIt implements Iterator<Integer> {
		private int i = 0;

		@Override
		public boolean hasNext() {
			return i < len;
		}

		@Override
		public Integer next() {
			if (i == len)
				throw new NoSuchElementException();
			return keys[i++];
		}

	}

	private class Values implements Collection<T> {

		@Override
		public int size() {
			return len;
		}

		@Override
		public boolean isEmpty() {
			return len == 0;
		}

		@Override
		public boolean contains(Object o) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Iterator<T> iterator() {
			return new ValuesIt();
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <E> E[] toArray(E[] a) {
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends T> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}

	private class ValuesIt implements Iterator<T> {
		private int i = 0;

		@Override
		public boolean hasNext() {
			return i < len;
		}

		@Override
		public T next() {
			if (i == len)
				throw new NoSuchElementException();
			return values[i++];
		}

	}
}
