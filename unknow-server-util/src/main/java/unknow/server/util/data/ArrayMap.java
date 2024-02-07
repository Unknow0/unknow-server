/**
 * 
 */
package unknow.server.util.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author unknow
 */
public class ArrayMap<T> implements Map<String, T> {
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
	 * create a new arrayMap with these key/value /!\ key should already be sorted
	 * @param key the keys
	 * @param value the value
	 */
	public ArrayMap(String[] key, T[] value) {
		if (key.length != value.length)
			throw new IllegalArgumentException("different number of key and value");
		this.key = key;
		this.value = value;
		this.len = key.length;
	}

	@Override
	public T get(Object key) {
		return key instanceof String ? get((String) key) : null;
	}

	/**
	 * get a key
	 * @param name the key
	 * @return the associated value or null
	 */
	public T get(String name) {
		int i = Arrays.binarySearch(key, 0, len, name);
		return i < 0 ? null : value[i];
	}

	/**
	 * get enumeration of keys
	 * @return the keys
	 */
	public Enumeration<String> names() {
		return new E();
	}

	@Override
	public int size() {
		return key.length;
	}

	@Override
	public T put(String name, T o) {
		int i = Arrays.binarySearch(key, 0, len, name);
		if (i >= 0) {
			value[i] = o;
			return null;
		}
		i = -i - 1;
		if (i < len) {
			System.arraycopy(key, i, key, i + 1, len - i);
			System.arraycopy(value, i, value, i + 1, len - i);
		}
		ensure(++len);
		T old = value[i];
		key[i] = name;
		value[i] = o;
		return old;
	}

	/**
	 * put a value only if not present
	 * @param name the key
	 * @param o the value
	 * @return true if the value was added
	 */
	public boolean putOnce(String name, T o) {
		int i = Arrays.binarySearch(key, 0, len, name);
		if (i >= 0)
			return false;
		i = -i - 1;
		if (i < len) {
			System.arraycopy(key, i, key, i + 1, len - i);
			System.arraycopy(value, i, value, i + 1, len - i);
		}
		ensure(++len);
		key[i] = name;
		value[i] = o;
		return true;
	}

	@Override
	public T remove(Object name) {
		return name instanceof String ? remove((String) name) : null;
	}

	/**
	 * remove a key
	 * @param name the key
	 * @return the old value
	 */
	public T remove(String name) {
		return put(name, null);
	}

	private void ensure(int l) {
		if (l <= key.length)
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

	@Override
	public boolean isEmpty() {
		return key.length == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		for (int i = 0; i < this.value.length; i++) {
			if (this.value[i].equals(value))
				return true;
		}
		return false;
	}

	@Override
	public void putAll(Map<? extends String, ? extends T> m) {
		for (Entry<? extends String, ? extends T> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
		return new KeySet();
	}

	@Override
	public Collection<T> values() {
		return Arrays.asList(value);
	}

	@Override
	public Set<Entry<String, T>> entrySet() {
		return new Entries();
	}

	private class KeySet implements Set<String> {
		@Override
		public int size() {
			return ArrayMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return ArrayMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return ArrayMap.this.containsKey(o);
		}

		@Override
		public Iterator<String> iterator() {
			return new ArrayIt<>(key);
		}

		@Override
		public Object[] toArray() {
			return Arrays.copyOf(key, key.length);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <R> R[] toArray(R[] a) {
			R[] r = a.length >= key.length ? a : (R[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), key.length);
			System.arraycopy(key, 0, r, 0, key.length);
			if (a.length > key.length)
				r[key.length] = null;
			return r;
		}

		@Override
		public boolean add(String e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			return ArrayMap.this.remove(o) != null;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!ArrayMap.this.containsKey(o))
					return false;
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends String> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			boolean r = false;
			for (Object o : c)
				r |= remove(o);
			return r;
		}

		@Override
		public void clear() {
			ArrayMap.this.clear();
		}
	}

	private class Entries implements Set<Map.Entry<String, T>> {
		@Override
		public int size() {
			return ArrayMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return ArrayMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return ArrayMap.this.containsKey(o);
		}

		@Override
		public Iterator<Map.Entry<String, T>> iterator() {
			return new EntriesIt();
		}

		@Override
		public Object[] toArray() {
			return Arrays.copyOf(key, key.length);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <R> R[] toArray(R[] a) {
			R[] r = a.length >= key.length ? a : (R[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), key.length);
			System.arraycopy(key, 0, r, 0, key.length);
			if (a.length > key.length)
				r[key.length] = null;
			return r;
		}

		@Override
		public boolean add(Map.Entry<String, T> e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			return ArrayMap.this.remove(o) != null;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) {
				if (!ArrayMap.this.containsKey(o))
					return false;
			}
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends Map.Entry<String, T>> c) {
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
			ArrayMap.this.clear();
		}
	}

	private class EntriesIt implements Iterator<Map.Entry<String, T>> {
		private int i;

		public EntriesIt() {
			this.i = 0;
		}

		@Override
		public boolean hasNext() {
			return i < len;
		}

		@Override
		public Map.Entry<String, T> next() {
			if (i == len)
				throw new NoSuchElementException();
			return new MapEntry<>(key[i], value[i++]);
		}
	}
}
