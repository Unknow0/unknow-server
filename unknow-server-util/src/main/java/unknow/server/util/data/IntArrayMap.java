/**
 * 
 */
package unknow.server.util.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * int map backed up by sorted array
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

	public T getOrDefault(int key, T value) {
		T t = get(key);
		return t == null ? value : t;
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
		return new IntImmuableArrayList(keys, 0, len);
	}

	/**
	 * @return all values
	 */
	public Collection<T> values() {
		return new ImmuableArrayList<>(values, 0, len);
	}

	protected int keyAt(int i) {
		return keys[i];
	}

	protected T valueAt(int i) {
		return values[i];
	}
}
