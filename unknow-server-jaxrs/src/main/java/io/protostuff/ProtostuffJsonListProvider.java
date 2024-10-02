package io.protostuff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
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
import jakarta.ws.rs.ext.Provider;
import unknow.server.http.jaxrs.protostuff.ProtostuffSchema;

@Provider
@Priority(4500)
@Consumes({ "application/json" })
@Produces({ "application/json" })
public class ProtostuffJsonListProvider<T extends Message<?>> extends ProtostuffJsonListAbstract<T> {
	private static final JsonFactory FACTORY = JsonFactory.builder().build();

	private static final byte[] EMPTY = { '[', ']' };

	@Override
	protected void writeEmpty(OutputStream out) throws IOException {
		out.write(EMPTY);
	}

	@Override
	protected void writeStart(JsonXOutput output) throws IOException {
		output.writeStartArray();
	}

	@Override
	protected void writeSeparator(JsonXOutput output) throws IOException {
		output.tail = output.sink.writeByte((byte) ',', output, output.tail);
	}

	@Override
	protected void writeEnd(JsonXOutput output) throws IOException {
		output.writeEndArray();
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
