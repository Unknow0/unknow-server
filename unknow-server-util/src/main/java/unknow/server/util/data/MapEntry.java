/**
 * 
 */
package unknow.server.util.data;

import java.util.Map;

/** a map entry */
public class MapEntry<K, V> implements Map.Entry<K, V> {
	private final K key;
	private final V value;

	/**
	 * @param key the key
	 * @param value the value
	 */
	public MapEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}
}