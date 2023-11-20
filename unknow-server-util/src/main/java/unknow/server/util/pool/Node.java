package unknow.server.util.pool;

public final class Node<T> {
	private static final Object mutex = new Object();
	@SuppressWarnings("rawtypes")
	private static Node idle = null;

	public T t;
	public Node<T> n;

	private Node() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Node<T> get() {
		synchronized (mutex) {
			Node n = idle;
			if (n == null)
				return new Node<>();
			idle = n.n;
			return n;
		}
	}

	@SuppressWarnings("unchecked")
	public static void free(Node<?> n) {
		synchronized (mutex) {
			n.t = null;
			n.n = idle;
			idle = n;
		}
	}
}