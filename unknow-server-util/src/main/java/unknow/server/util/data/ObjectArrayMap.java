/**
 * 
 */
package unknow.server.util.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author unknow
 */
public class ObjectArrayMap<K, V> implements Map<K, V> {
	private final Comparator<K> cmp;
	private K[] key;
	private V[] value;
	private int len;

	/**
	 * create an empty array map
	 */
	@SuppressWarnings("unchecked")
	public ObjectArrayMap(Comparator<K> cmp) {
		this.cmp = cmp;
		key = (K[]) new Object[10];
		value = (V[]) new Object[10];
		len = 0;
	}

	/**
	 * create a new arrayMap with these key/value /!\ key should already be sorted
	 */
	public ObjectArrayMap(K[] key, V[] value, Comparator<K> cmp) {
		if (key.length != value.length)
			throw new IllegalArgumentException("different number of key and value");
		this.cmp = cmp;
		this.key = key;
		this.value = value;
		this.len = key.length;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object name) {
		int i = Arrays.binarySearch(key, 0, len, (K) name, cmp);
		return i < 0 ? null : value[i];
	}

	public Enumeration<K> keys() {
		return new E();
	}

	@Override
	public V put(K name, V o) {
		int i = Arrays.binarySearch(key, 0, len, name, cmp);
		if (i >= 0) {
			value[i] = o;
			return null;
		}
		if (i < len) {
			System.arraycopy(key, i, key, i + 1, len - i);
			System.arraycopy(value, i, value, i + 1, len - i);
		}
		ensure(++len);
		i = -i - 1;
		V old = value[i];
		key[i] = name;
		value[i] = o;
		return old;
	}

	public boolean putOnce(K name, V o) {
		int i = Arrays.binarySearch(key, 0, len, name, cmp);
		if (i >= 0)
			return false;
		if (i < len) {
			System.arraycopy(key, i, key, i + 1, len - i);
			System.arraycopy(value, i, value, i + 1, len - i);
		}
		ensure(++len);
		i = -i - 1;
		key[i] = name;
		value[i] = o;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object name) {
		return put((K) name, null);
	}

	@Override
	public void clear() {
		len = 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new EntrySet();
	}

	@Override
	public boolean isEmpty() {
		return len == 0;
	}

	@Override
	public Set<K> keySet() {
		return new KeySet();
	}

	@Override
	public Collection<V> values() {
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		ensure(len + m.size());
		for (Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override
	public int size() {
		return len;
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

	private class EntrySet implements Set<Entry<K, V>> {

		@Override
		public int size() {
			return ObjectArrayMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return ObjectArrayMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new EntryIt();
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean add(Entry<K, V> e) {
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
		public boolean addAll(Collection<? extends Entry<K, V>> c) {
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
			ObjectArrayMap.this.clear();
		}
	}

	private class EntryIt implements Iterator<Map.Entry<K, V>>, Map.Entry<K, V> {
		private int i = 0;

		@Override
		public boolean hasNext() {
			return i < len;
		}

		@Override
		public Map.Entry<K, V> next() {
			if (i == len)
				throw new NoSuchElementException();
			return this;
		}

		@Override
		public K getKey() {
			return key[i++];
		}

		@Override
		public V getValue() {
			return value[i];
		}

		@Override
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}

	}

	private class KeySet implements Set<K> {

		@Override
		public int size() {
			return ObjectArrayMap.this.size();
		}

		@Override
		public boolean isEmpty() {
			return ObjectArrayMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return false;
		}

		@Override
		public Iterator<K> iterator() {
			return new KeyIt();
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean add(K e) {
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
		public boolean addAll(Collection<? extends K> c) {
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
			ObjectArrayMap.this.clear();
		}
	}

	private class KeyIt implements Iterator<K> {
		private int i = 0;

		@Override
		public boolean hasNext() {
			return i < len;
		}

		@Override
		public K next() {
			if (i == len)
				throw new NoSuchElementException();
			return key[i++];
		}

	}
}
