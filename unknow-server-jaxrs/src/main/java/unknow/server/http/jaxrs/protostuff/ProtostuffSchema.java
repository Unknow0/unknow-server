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

	public static <T extends Message> Schema<T> get(Type type) {
		if (!(type instanceof Class))
			throw new IllegalArgumentException("No schema for type " + type);
		Class cl = (Class) type;
		return SCHEMA.computeIfAbsent(cl, ProtostuffSchema::createSchema);
	}

	private static <S extends Message> Schema<S> createSchema(Class<S> clazz) {
		try {
			return clazz.getDeclaredConstructor().newInstance().cachedSchema();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}
