/**
 * 
 */
package unknow.server.util.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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
			values[i] = value;
			return null;
		}
		if (i < len) {
			System.arraycopy(keys, i, keys, i + 1, len - i);
			System.arraycopy(values, i, values, i + 1, len - i);
		}
		ensure(len++);
		i = -i - 1;
		T old = values[i];
		keys[i] = key;
		values[i] = value;
		return old;
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
		if (i < len) {
			System.arraycopy(keys, i, keys, i + 1, len - i);
			System.arraycopy(values, i, values, i + 1, len - i);
		}
		ensure(len++);
		i = -i - 1;
		keys[i] = key;
		values[i] = value;
		return true;
	}

	/**
	 * @param key the key to remove
	 * @return the removed value
	 */
	public T remove(int key) {
		return set(key, null);
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
	 * @return set of all the keys
	 */
	public Set<Integer> keySet() {
		return new KeySet();
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
			return keys[i++];
		}

	}
}
