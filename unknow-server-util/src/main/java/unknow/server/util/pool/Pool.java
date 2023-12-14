package unknow.server.util.pool;

import java.util.function.Supplier;

/**
 * basic unsynchronized pool
 * @param <T> element type
 */
public interface Pool<T> extends Supplier<T> {
	/**
	 * get or create a new element
	 * @return an idle or a new element
	 */
	@Override
	T get();

	/**
	 * put back an element in the pool
	 * @param t element to free
	 */
	void free(T t);
}