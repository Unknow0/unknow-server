package unknow.server.util.pool;

import java.util.function.Function;

/**
 * create a new pool per thread
 * @param max max idle elements
 * @param s supplier of newly created element
 */
public class LocalPool<T> implements Pool<T> {
	private final ThreadLocal<Pool<T>> l;

	public LocalPool(int max, Function<Pool<T>, ? extends T> s) {
		l = ThreadLocal.withInitial(() -> new BasePool<>(max, s));
	}

	@Override
	public final T get() {
		return l.get().get();
	}

	@Override
	public void free(T t) {
		l.get().free(t);
	}
}
