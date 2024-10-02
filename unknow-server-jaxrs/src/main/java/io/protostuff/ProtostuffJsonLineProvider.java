package io.protostuff;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.core.JsonParser;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(4500)
@Consumes({ "application/x-ndjson", "application/jsonl" })
@Produces({ "application/x-ndjson", "application/jsonl" })
public class ProtostuffJsonLineProvider<T extends Message<?>> extends ProtostuffJsonListAbstract<T> {

	@Override
	protected void writeEmpty(OutputStream out) {
		// nothing
	}

	@Override
	protected void writeStart(JsonXOutput output) throws IOException {
		// nothing
	}

	@Override
	protected void writeSeparator(JsonXOutput output) throws IOException {
		output.tail = output.sink.writeByte((byte) '\n', output, output.tail);
	}

	@Override
	protected void writeEnd(JsonXOutput output) throws IOException {
		output.tail = output.sink.writeByte((byte) '\n', output, output.tail);
	}

	@Override
	protected void readStart(JsonParser parser) {
		// nothing
	}
}
