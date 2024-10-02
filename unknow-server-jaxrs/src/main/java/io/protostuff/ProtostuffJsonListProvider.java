package io.protostuff;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(4500)
@Consumes({ "application/json" })
@Produces({ "application/json" })
public class ProtostuffJsonListProvider<T extends Message<?>> extends ProtostuffJsonListAbstract<T> {

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
	protected void readStart(JsonParser parser) throws IOException {
		if (parser.nextToken() != JsonToken.START_ARRAY)
			throw new JsonInputException("Expected token: [ but was " + parser.getCurrentToken());

	}
}
