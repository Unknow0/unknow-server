package unknow.server.util.pool;

public final class Node<T> {
	@SuppressWarnings("rawtypes")
	private static final ThreadLocal<Node> idle = new ThreadLocal<>();

	public T t;
	public Node<T> n;

	private Node() {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Node<T> get() {
		Node n = idle.get();
		if (n == null)
			return new Node<>();
		idle.set(n.n);
		return n;
	}

	@SuppressWarnings("unchecked")
	public static void free(Node<?> n) {
		n.t = null;
		n.n = idle.get();
		idle.set(n);
	}
}