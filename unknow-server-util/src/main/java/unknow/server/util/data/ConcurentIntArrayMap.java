/**
 * 
 */
package unknow.server.util.data;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author unknow
 * @param <T> value type
 */
public class ConcurentIntArrayMap<T> extends IntArrayMap<T> {
	private final ReadWriteLock lock;

	/**
	 * create an empty array map
	 */
	public ConcurentIntArrayMap() {
		lock = new ReentrantReadWriteLock();
	}

	/**
	 * create a new arrayMap with these key/value /!\ key should already be sorted
	 * 
	 * @param key   the keys
	 * @param value the values
	 */
	public ConcurentIntArrayMap(int[] key, T[] value) {
		super(key, value);
		lock = new ReentrantReadWriteLock();
	}

	/**
	 * @param key the key
	 * @return true if the key exists
	 */
	@Override
	public boolean contains(int key) {
		lock.readLock().lock();
		try {
			return super.contains(key);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @param key the key
	 * @return the associated value
	 */
	@Override
	public T get(int key) {
		lock.readLock().lock();
		try {
			return super.get(key);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * set a value
	 * 
	 * @param key   the key
	 * @param value the value
	 * @return the old value
	 */
	@Override
	public T set(int key, T value) {
		lock.writeLock().lock();
		try {
			return super.set(key, value);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * set the key only if missing
	 * 
	 * @param key   the key
	 * @param value the value
	 * @return true if the map changed
	 */
	@Override
	public boolean setOnce(int key, T value) {
		lock.writeLock().lock();
		try {
			return super.setOnce(key, value);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * @param key the key to remove
	 * @return the removed value
	 */
	@Override
	public T remove(int key) {
		lock.writeLock().lock();
		try {
			return super.remove(key);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * empty the map
	 */
	@Override
	public void clear() {
		lock.writeLock().lock();
		try {
			super.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * @return map size
	 */
	@Override
	public int size() {
		lock.readLock().lock();
		try {
			return super.size();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return true if map is empty
	 */
	@Override
	public boolean isEmpty() {
		lock.readLock().lock();
		try {
			return super.isEmpty();
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	protected int keyAt(int i) {
		lock.readLock().lock();
		try {
			return super.keyAt(i);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	protected T valueAt(int i) {
		lock.readLock().lock();
		try {
			return super.valueAt(i);
		} finally {
			lock.readLock().unlock();
		}
	}
}
