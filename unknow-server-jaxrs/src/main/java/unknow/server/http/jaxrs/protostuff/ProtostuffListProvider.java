package unknow.server.http.jaxrs.protostuff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import io.protostuff.LinkedBuffer;
import io.protostuff.Message;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.ProtobufOutput;
import io.protostuff.Schema;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(4500)
@Consumes({ "application/x-protobuf" })
@Produces({ "application/x-protobuf" })
public class ProtostuffListProvider<T extends Message<?>> implements MessageBodyReader<Collection<T>>, MessageBodyWriter<Collection<T>> {

	private static final byte[] EMPTY = { '[', ']' };

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		if (!(genericType instanceof ParameterizedType))
			return false;
		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];
		return p instanceof Class && Message.class.isAssignableFrom((Class<?>) p);
	}

	@Override
	public void writeTo(Collection<T> list, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
			OutputStream out) throws IOException, WebApplicationException {
		if (list.isEmpty()) {
			out.write(EMPTY);
			return;
		}

		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];

		Schema<T> schema = ProtostuffSchema.get(p);

		LinkedBuffer buffer = LinkedBuffer.allocate(4096);
		final ProtobufOutput output = new ProtobufOutput(buffer);
		for (T t : list) {
			schema.writeTo(output, t);
			final int size = output.getSize();
			ProtobufOutput.writeRawVarInt32Bytes(out, size);
			LinkedBuffer.writeTo(out, buffer);
			output.clear();
		}
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return isWriteable(type, genericType, annotations, mediaType);
	}

	@Override
	public Collection<T> readFrom(Class<Collection<T>> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
			InputStream in) throws IOException, WebApplicationException {
		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];
		Schema<T> schema = ProtostuffSchema.get(p);
		return ProtobufIOUtil.parseListFrom(in, schema);
	}
}
