package unknow.server.http.jaxrs.protostuff;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.protostuff.Message;
import io.protostuff.Schema;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ProtostuffSchema {
	private static final Map<Class, Schema> SCHEMA = new ConcurrentHashMap<>();

	private ProtostuffSchema() {
	}

	public static <T> void register(Class<T> clazz, Schema<T> schema) {
		SCHEMA.put(clazz, schema);
	}

	public static <T> Schema<T> get(Type type) {
		if (!(type instanceof Class))
			throw new IllegalArgumentException("No schema for type " + type);
		Class cl = (Class) type;
		return SCHEMA.computeIfAbsent(cl, ProtostuffSchema::createSchema);
	}

	private static <T> Schema<T> createSchema(Class<T> clazz) {
		try {
			if (!Message.class.isAssignableFrom(clazz))
				throw new IllegalArgumentException("Can't create schema of " + clazz);
			return ((Message) clazz.getDeclaredConstructor().newInstance()).cachedSchema();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}
