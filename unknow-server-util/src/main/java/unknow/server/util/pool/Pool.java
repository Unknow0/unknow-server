package unknow.server.util.pool;

/**
 * basic unsynchronized pool
 * @param <T> element type
 */
public interface Pool<T> {
	/**
	 * get or create a new element
	 * @return an idle or a new element
	 */
	T get();

	/**
	 * put back an element in the pool
	 * @param t element to free
	 */
	void free(T t);
}