/**
 * 
 */
package unknow.server.util.data;

import java.util.Map;

public class MapEntry<K, V> implements Map.Entry<K, V> {
	private final K key;
	private final V value;

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