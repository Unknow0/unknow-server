/**
 * 
 */
package unknow.server.http.utils;

import java.util.Arrays;

/**
 * @author unknow
 */
public class IntArrayMap<T> {
	private int[] key;
	private T[] value;
	private int len;

	/**
	 * create an empty array map
	 */
	@SuppressWarnings("unchecked")
	public IntArrayMap() {
		key = new int[10];
		value = (T[]) new Object[10];
		len = 0;
	}

	/**
	 * create a new arrayMap with these key/value /!\ key should already be sorted
	 */
	public IntArrayMap(int[] key, T[] value) {
		if (key.length != value.length)
			throw new IllegalArgumentException("different number of key and value");
		this.key = key;
		this.value = value;
		this.len = key.length;
	}

	public T get(int name) {
		int i = Arrays.binarySearch(key, 0, len, name);
		return i < 0 ? null : value[i];
	}

	public T set(int name, T o) {
		int i = Arrays.binarySearch(key, 0, len, name);
		if (i >= 0) {
			value[i] = o;
			return null;
		}
		if (i < len) {
			System.arraycopy(key, i, key, i + 1, len - i);
			System.arraycopy(value, i, value, i + 1, len - i);
		}
		ensure(len++);
		i = -i - 1;
		T old = value[i];
		key[i] = name;
		value[i] = o;
		return old;
	}

	public boolean setOnce(int name, T o) {
		int i = Arrays.binarySearch(key, 0, len, name);
		if (i >= 0)
			return false;
		if (i < len) {
			System.arraycopy(key, i, key, i + 1, len - i);
			System.arraycopy(value, i, value, i + 1, len - i);
		}
		ensure(len++);
		i = -i - 1;
		key[i] = name;
		value[i] = o;
		return true;
	}

	public T remove(int name) {
		return set(name, null);
	}

	private void ensure(int l) {
		if (l < key.length)
			return;
		key = Arrays.copyOf(key, l);
		value = Arrays.copyOf(value, l);
	}
}
