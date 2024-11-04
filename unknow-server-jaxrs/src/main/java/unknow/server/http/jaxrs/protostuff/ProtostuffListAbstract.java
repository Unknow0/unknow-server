package unknow.server.http.jaxrs.protostuff;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import io.protostuff.Message;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

public abstract class ProtostuffListAbstract<T> implements MessageBodyReader<Collection<T>>, MessageBodyWriter<Collection<T>> {
	@Override
	public final boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		if (!(genericType instanceof ParameterizedType))
			return false;
		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];
		return p instanceof Class && Message.class.isAssignableFrom((Class<?>) p);
	}

	@Override
	public final boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return isWriteable(type, genericType, annotations, mediaType);
	}
}
