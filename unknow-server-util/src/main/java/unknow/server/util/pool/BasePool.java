package unknow.server.util.pool;

import java.util.function.Function;

/**
 * basic unsynchronized pool
 * @param <T> element type
 */
public final class BasePool<T> implements Pool<T> {
	private final Function<Pool<T>, ? extends T> s;
	private final int max;
	private int len;

	private volatile Node<T> idle;

	/**
	 * create a new pool
	 * @param max max idle elements
	 * @param s supplier of newly created element
	 */
	public BasePool(int max, Function<Pool<T>, ? extends T> s) {
		this.s = s;
		this.max = max;
	}

	/**
	 * get or create a new element
	 * @return an idle or a new element
	 */
	@Override
	public T get() {
		if (idle == null)
			return s.apply(this);
		T t = idle.t;
		idle = idle.n;
		len--;
		return t;
	}

	public boolean contains(T t) {
		Node<T> n = idle;
		while (n != null) {
			if (n.t == t)
				return true;
			n = n.n;
		}
		return false;
	}

	/**
	 * put back an element in the pool
	 * @param t element to free
	 */
	@Override
	public void free(T t) {
		if (len == max)
			return;
		Node<T> n = Node.get();
		n.t = t;
		n.n = idle;
		idle = n;
		len++;
	}
}