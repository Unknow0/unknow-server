package unknow.server.util;

public interface ConsumerWithException<T, E extends Throwable> {
	void accept(T t) throws E;
}
