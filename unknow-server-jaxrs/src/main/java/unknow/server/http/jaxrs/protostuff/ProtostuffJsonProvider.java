package unknow.server.http.jaxrs.protostuff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.protostuff.JsonIOUtil;
import io.protostuff.JsonXIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.Message;
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
@Consumes({ "application/json", "application/x-ndjson", "application/jsonl" })
@Produces({ "application/json", "application/x-ndjson", "application/jsonl" })
@SuppressWarnings("rawtypes")
public class ProtostuffJsonProvider<T extends Message> implements MessageBodyReader<T>, MessageBodyWriter<T> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return Message.class.isAssignableFrom(type);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream out)
			throws IOException, WebApplicationException {
		LinkedBuffer buffer = LinkedBuffer.allocate(4096);
		JsonXIOUtil.writeTo(out, t, false, buffer);
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return Message.class.isAssignableFrom(type);
	}

	@Override
	public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream in)
			throws IOException, WebApplicationException {
		Schema<T> schema = ProtostuffSchema.get(type);
		T t = schema.newMessage();
		JsonIOUtil.mergeFrom(in, t, schema, false);
		return t;
	}
}
