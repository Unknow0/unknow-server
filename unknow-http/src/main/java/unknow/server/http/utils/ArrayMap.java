/**
 * 
 */
package unknow.server.http.utils;

import java.util.Arrays;
import java.util.Enumeration;

/**
 * @author unknow
 */
public class ArrayMap<T> {
	private String[] key;
	private T[] value;
	private int len;

	/**
	 * create an empty array map
	 */
	@SuppressWarnings("unchecked")
	public ArrayMap() {
		key = new String[10];
		value = (T[]) new Object[10];
		len = 0;
	}

	/**
	 * create a new arrayMap with these key/value
	 */
	public ArrayMap(String[] key, T[] value) {
		if (key.length != value.length)
			throw new IllegalArgumentException("different number of key and value");
		this.key = key;
		this.value = value;
		this.len = key.length;
	}

	public T get(String name) {
		int i = Arrays.binarySearch(key, 0, len, name);
		return i < 0 ? null : value[i];
	}

	public Enumeration<String> names() {
		return new E();
	}

	public T set(String name, T o) {
		int i = Arrays.binarySearch(key, 0, len, name);
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

	public boolean setOnce(String name, T o) {
		int i = Arrays.binarySearch(key, 0, len, name);
		if (i >= 0)
			return false;
		ensure(len++);
		i = -i - 1;
		key[i] = name;
		value[i] = o;
		return true;
	}

	public T remove(String name) {
		return set(name, null);
	}

	private void ensure(int l) {
		if (l < key.length)
			return;
		key = Arrays.copyOf(key, l);
		value = Arrays.copyOf(value, l);
	}

	private class E implements Enumeration<String> {
		private int i = 0;

		@Override
		public boolean hasMoreElements() {
			return i < len;
		}

		@Override
		public String nextElement() {
			return key[i++];
		}

	}
}
