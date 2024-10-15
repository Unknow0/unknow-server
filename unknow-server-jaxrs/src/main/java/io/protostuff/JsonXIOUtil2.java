package io.protostuff;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JsonXIOUtil2 {
	private static final byte[] EMPTY_ARRAY = { '[', ']' };

	private JsonXIOUtil2() {
	}

	private static void write(JsonXOutput o, byte[] b) throws IOException {
		if (b.length == 0)
			return;
		if (b.length == 1)
			o.tail = o.sink.writeByte(b[0], o, o.tail);
		else
			o.tail = o.sink.writeByteArray(b, o, o.tail);
	}

	/**
	* Serializes the {@code messages} into the {@link LinkedBuffer} using the given schema.
	*/
	public static <T> void writeListTo(LinkedBuffer buffer, Collection<T> messages, Schema<T> schema, boolean numeric, ListFormat fmt) throws IOException {
		if (buffer.start != buffer.offset)
			throw new IllegalArgumentException("Buffer previously used and had not been reset.");

		if (messages.isEmpty()) {
			System.arraycopy(EMPTY_ARRAY, 0, buffer.buffer, buffer.offset, EMPTY_ARRAY.length);
			buffer.offset += EMPTY_ARRAY.length;
			return;
		}

		final JsonXOutput output = new JsonXOutput(buffer, numeric, schema);
		boolean first = true;
		for (T m : messages) {
			if (first) {
				first = false;
				write(output, fmt.start());
			} else
				write(output, fmt.delimiter());

			output.writeStartObject();
			schema.writeTo(output, m);
			if (output.isLastRepeated())
				output.writeEndArray();

			output.writeEndObject().reset();
		}

		write(output, fmt.end());
	}

	/**
	 * Serializes the {@code messages} into the stream using the given schema with the supplied buffer.
	 */
	public static <T> void writeListTo(OutputStream out, Collection<T> messages, Schema<T> schema, boolean numeric, LinkedBuffer buffer, ListFormat fmt) throws IOException {
		if (buffer.start != buffer.offset)
			throw new IllegalArgumentException("Buffer previously used and had not been reset.");

		if (messages.isEmpty()) {
			System.arraycopy(EMPTY_ARRAY, 0, buffer.buffer, buffer.offset, EMPTY_ARRAY.length);
			buffer.offset += EMPTY_ARRAY.length;
			return;
		}

		final JsonXOutput output = new JsonXOutput(buffer, out, numeric, schema);

		boolean first = true;
		for (T m : messages) {
			if (first) {
				first = false;
				write(output, fmt.start());
			} else
				write(output, fmt.delimiter());

			schema.writeTo(output, m);
			if (output.isLastRepeated())
				output.writeEndArray();

			output.writeEndObject().reset();
		}
		write(output, fmt.end());
		LinkedBuffer.writeTo(out, buffer);
	}

	public static <T> void mergeFrom(byte[] data, T message, Schema<T> schema, boolean numeric) throws IOException {
		mergeFrom(data, 0, data.length, message, schema, numeric);
	}

	/**
	 * Merges the {@code message} with the byte array using the given {@code schema}.
	 */
	public static <T> void mergeFrom(byte[] data, int offset, int length, T message, Schema<T> schema, boolean numeric) throws IOException {
		mergeFrom(new ByteArrayInputStream(data, offset, length), message, schema, numeric);
	}

	/**
	 * Merges the {@code message} from the {@link InputStream} using the given {@code schema}.
	 */
	public static <T> void mergeFrom(InputStream in, T message, Schema<T> schema, boolean numeric) throws IOException {
		JsonXInput input = new JsonXInput(in, numeric);
		input.readStartObject();
		schema.mergeFrom(input, message);
	}

	/**
	* Parses the {@code messages} from the stream using the given {@code schema}.
	*/
	public static <T> List<T> parseListFrom(InputStream in, Schema<T> schema, boolean numeric) throws IOException {
		final JsonXInput input = new JsonXInput(in, numeric);
		input.readStartArray();

		final List<T> list = new ArrayList<T>();
		if (input.isNext(']'))
			return list;

		do {
			input.readStartObject();

			final T message = schema.newMessage();
			schema.mergeFrom(input, message);
			list.add(message);
			input.reset();
		} while (input.isNext(','));
		input.readEndArray();
		return list;
	}

	public static enum ListFormat {
		JSON(new byte[] { '[' }, new byte[] { ',' }, new byte[] { ']' }), NDJSON(new byte[] {}, new byte[] { '\n' }, new byte[] {});

		private final byte[] start;
		private final byte[] delimiter;
		private final byte[] end;

		private ListFormat(byte[] start, byte[] delimiter, byte[] end) {
			this.start = start;
			this.delimiter = delimiter;
			this.end = end;
		}

		public byte[] start() {
			return start;
		}

		public byte[] delimiter() {
			return delimiter;
		}

		public byte[] end() {
			return end;
		}
	}
}
