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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import unknow.server.http.jaxrs.protostuff.ProtostuffListAbstract;
import unknow.server.http.jaxrs.protostuff.ProtostuffSchema;

public abstract class ProtostuffJsonListAbstract<T extends Message<?>> extends ProtostuffListAbstract<T> {
	private static final JsonFactory FACTORY = JsonFactory.builder().build();

	protected abstract void writeEmpty(OutputStream out) throws IOException;

	protected abstract void writeEnd(JsonXOutput output) throws IOException;

	protected abstract void writeSeparator(JsonXOutput output) throws IOException;

	protected abstract void writeStart(JsonXOutput output) throws IOException;

	@Override
	public final void writeTo(Collection<T> t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
			OutputStream out) throws IOException, WebApplicationException {
		if (t.isEmpty()) {
			writeEmpty(out);
			return;
		}

		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];

		LinkedBuffer buffer = LinkedBuffer.allocate(4096);
		Schema<T> schema = ProtostuffSchema.get(p);
		final JsonXOutput output = new JsonXOutput(buffer, out, false, schema);

		writeStart(output);

		Iterator<T> it = t.iterator();
		output.writeStartObject();
		schema.writeTo(output, it.next());
		if (output.isLastRepeated())
			output.writeEndArray();
		output.writeEndObject();
		while (it.hasNext()) {
			writeSeparator(output);
			output.writeStartObject();
			schema.writeTo(output, it.next());
			if (output.isLastRepeated())
				output.writeEndArray();
			output.writeEndObject();
		}

		writeEnd(output);
		LinkedBuffer.writeTo(out, buffer);
	}

	protected abstract void readStart(JsonParser parser) throws IOException;

	@Override
	public final Collection<T> readFrom(Class<Collection<T>> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
			InputStream in) throws IOException, WebApplicationException {
		Type p = ((ParameterizedType) genericType).getActualTypeArguments()[0];

		Schema<T> schema = ProtostuffSchema.get(p);
		List<T> list = new ArrayList<T>();
		try (JsonParser parser = FACTORY.createParser(in)) {
			readStart(parser);

			JsonInput input = new JsonInput(parser, false);
			JsonToken t;
			while ((t = parser.nextToken()) != JsonToken.END_ARRAY && t != null) {
				if (t != JsonToken.START_OBJECT)
					throw new JsonInputException("Expected token: { but was " + parser.getCurrentToken() + " on message " + schema.messageFullName());

				final T message = schema.newMessage();
				schema.mergeFrom(input, message);

				if (parser.getCurrentToken() != JsonToken.END_OBJECT)
					throw new JsonInputException("Expected token: } but was " + parser.getCurrentToken() + " on message " + schema.messageFullName());

				list.add(message);
				input.reset();
			}
			return list;
		}
	}
}
