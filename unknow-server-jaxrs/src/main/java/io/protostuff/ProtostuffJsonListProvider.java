package io.protostuff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import unknow.server.http.jaxrs.protostuff.ProtostuffSchema;

@Provider
@Priority(4500)
@Consumes({ "application/json"/*, "application/x-ndjson", "application/jsonl"*/ })
@Produces({ "application/json"/*, "application/x-ndjson", "application/jsonl"*/ })
@SuppressWarnings("rawtypes")
public class ProtostuffJsonListProvider<T extends Message> implements MessageBodyReader<Collection<T>>, MessageBodyWriter<Collection<T>> {
	private static final JsonFactory FACTORY = JsonFactory.builder().build();

	private static final byte[] EMPTY = { '[', ']' };

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		if (!(genericType instanceof ParameterizedType))
			return false;
		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];
		return p instanceof Class && Message.class.isAssignableFrom((Class) p);
	}

	@Override
	public void writeTo(Collection<T> t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
			OutputStream out) throws IOException, WebApplicationException {
		if (t.isEmpty()) {
			out.write(EMPTY);
			return;
		}

		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];

		LinkedBuffer buffer = LinkedBuffer.allocate(4096);
		Schema<T> schema = ProtostuffSchema.get(p);
		final JsonXOutput output = new JsonXOutput(buffer, out, false, schema);

		output.writeStartArray();
		output.writeStartObject();

		Iterator<T> it = t.iterator();
		schema.writeTo(output, it.next());
		if (output.isLastRepeated())
			output.writeEndArray();
		output.writeEndObject();
		while (it.hasNext()) {
			output.writeCommaAndStartObject();
			schema.writeTo(output, it.next());
			if (output.isLastRepeated())
				output.writeEndArray();
			output.writeEndObject();
		}

		output.writeEndArray();
		LinkedBuffer.writeTo(out, buffer);
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
		List<T> list = new ArrayList<T>();
		try (JsonParser parser = FACTORY.createParser(in)) {
			if (parser.nextToken() != JsonToken.START_ARRAY)
				throw new JsonInputException("Expected token: [ but was " + parser.getCurrentToken() + " on message: " + schema.messageFullName());

			JsonInput input = new JsonInput(parser, false);
			JsonToken t;
			while ((t = parser.nextToken()) != JsonToken.END_ARRAY) {
				if (t != JsonToken.START_OBJECT)
					throw new JsonInputException("Expected token: { but was " + parser.getCurrentToken() + " on message " + schema.messageFullName());

				final T message = schema.newMessage();
				schema.mergeFrom(input, message);

				if (parser.getCurrentToken() != JsonToken.END_OBJECT) {
					throw new JsonInputException("Expected token: } but was " + parser.getCurrentToken() + " on message " + schema.messageFullName());
				}

				list.add(message);
				input.reset();
			}
			return list;
		}
	}
}
