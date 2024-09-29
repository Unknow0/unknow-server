package unknow.server.http.jaxrs.protostuff;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.protostuff.Message;
import io.protostuff.Schema;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ProtostuffSchema {
	private static final Map<Class, Schema> SCHEMA = new ConcurrentHashMap<>();

	public static <T extends Message> Schema<T> get(Class<T> clazz) {
		return SCHEMA.computeIfAbsent(clazz, ProtostuffSchema::createSchema);
	}

	private static <S extends Message> Schema<S> createSchema(Class<S> clazz) {
		try {
			return clazz.getDeclaredConstructor().newInstance().cachedSchema();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
