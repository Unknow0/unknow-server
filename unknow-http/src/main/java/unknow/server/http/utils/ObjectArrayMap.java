/**
 * 
 */
package unknow.server.http.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;

/**
 * @author unknow
 */
public class ObjectArrayMap<K, T> {
	private final Comparator<K> cmp;
	private K[] key;
	private T[] value;
	private int len;

	/**
	 * create an empty array map
	 */
	@SuppressWarnings("unchecked")
	public ObjectArrayMap(Comparator<K> cmp) {
		this.cmp = cmp;
		key = (K[]) new Object[10];
		value = (T[]) new Object[10];
		len = 0;
	}

	/**
	 * create a new arrayMap with these key/value /!\ key should already be sorted
	 */
	public ObjectArrayMap(K[] key, T[] value, Comparator<K> cmp) {
		if (key.length != value.length)
			throw new IllegalArgumentException("different number of key and value");
		this.cmp = cmp;
		this.key = key;
		this.value = value;
		this.len = key.length;
	}

	@SuppressWarnings("unchecked")
	public T get(K name) {
		int i = Arrays.binarySearch(key, 0, len, name, cmp);
		return i < 0 ? null : value[i];
	}

	public Enumeration<K> keys() {
		return new E();
	}

	@SuppressWarnings("unchecked")
	public T set(K name, T o) {
		int i = Arrays.binarySearch(key, 0, len, name, cmp);
		if (i >= 0) {
			value[i] = o;
			return null;
		}
		ensure(len++);
		i = -i - 1;
		T old = value[i];
		key[i] = name;
		value[i] = o;
		return old;
	}

	@SuppressWarnings("unchecked")
	public boolean setOnce(K name, T o) {
		int i = Arrays.binarySearch(key, 0, len, name, cmp);
		if (i >= 0)
			return false;
		ensure(len++);
		i = -i - 1;
		key[i] = name;
		value[i] = o;
		return true;
	}

	public T remove(K name) {
		return set(name, null);
	}

	private void ensure(int l) {
		if (l < key.length)
			return;
		key = Arrays.copyOf(key, l);
		value = Arrays.copyOf(value, l);
	}

	private class E implements Enumeration<K> {
		private int i = 0;

		@Override
		public boolean hasMoreElements() {
			return i < len;
		}

		@Override
		public K nextElement() {
			return key[i++];
		}

	}
}
