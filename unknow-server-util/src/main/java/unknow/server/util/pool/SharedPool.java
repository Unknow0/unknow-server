package unknow.server.util.pool;

import java.util.function.Function;

/**
 * create a new pool per thread
 * @param max max idle elements
 * @param s supplier of newly created element
 */
public class SharedPool<T> implements Pool<T> {
	private final BasePool<T> pool;

	public SharedPool(int max, Function<Pool<T>, ? extends T> s) {
		pool = new BasePool<>(max, s);
	}

	@Override
	public final T get() {
		synchronized (pool) {
			return pool.get();
		}
	}

	@Override
	public void free(T t) {
		synchronized (pool) {
			pool.free(t);
		}
	}
}
