package io.protostuff;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JsonXIOUtil2 {
	private static final byte[] EMPTY_ARRAY = { '[', ']' };

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
	public static <T> void writeListTo(LinkedBuffer buffer, Collection<T> messages, Schema<T> schema, boolean numeric, byte[] start, byte[] delimiter, byte[] end) {
		if (buffer.start != buffer.offset)
			throw new IllegalArgumentException("Buffer previously used and had not been reset.");

		if (messages.isEmpty()) {
			System.arraycopy(EMPTY_ARRAY, 0, buffer.buffer, buffer.offset, EMPTY_ARRAY.length);
			buffer.offset += EMPTY_ARRAY.length;
			return;
		}

		final JsonXOutput output = new JsonXOutput(buffer, numeric, schema);
		try {
			boolean first = true;
			for (T m : messages) {
				if (first) {
					first = false;
					write(output, start);
				} else
					write(output, delimiter);

				output.writeStartObject();
				schema.writeTo(output, m);
				if (output.isLastRepeated())
					output.writeEndArray();

				output.writeEndObject().reset();
			}

			write(output, end);
		} catch (IOException e) {
			throw new RuntimeException("Serializing to a byte array threw an IOException (should never happen).", e);
		}
	}

	/**
	 * Serializes the {@code messages} into the stream using the given schema with the supplied buffer.
	 */
	public static <T> void writeListTo(OutputStream out, Collection<T> messages, Schema<T> schema, boolean numeric, LinkedBuffer buffer, byte[] start, byte[] delimiter,
			byte[] end) throws IOException {
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
				write(output, start);
			} else
				write(output, delimiter);

			schema.writeTo(output, m);
			if (output.isLastRepeated())
				output.writeEndArray();

			output.writeEndObject().reset();
		}
		write(output, end);
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
		mergeFrom(new InputStreamReader(in, StandardCharsets.UTF_8), message, schema, numeric);
	}

	/**
	 * Merges the {@code message} from the {@link Reader} using the given {@code schema}.
	 */
	public static <T> void mergeFrom(Reader reader, T message, Schema<T> schema, boolean numeric) throws IOException {
		JsonXInput in = new JsonXInput(reader, numeric);
		in.readStartObject();
		schema.mergeFrom(in, message);
		in.readEndObject();
	}

	/**
	* Parses the {@code messages} from the stream using the given {@code schema}.
	*/
	public static <T> List<T> parseListFrom(InputStream in, Schema<T> schema, boolean numeric) throws IOException {
		return parseListFrom(new InputStreamReader(in, StandardCharsets.UTF_8), schema, numeric);
	}

	/**
	 * Parses the {@code messages} from the reader using the given {@code schema}.
	 */
	public static <T> List<T> parseListFrom(Reader reader, Schema<T> schema, boolean numeric) throws IOException {
		final JsonXInput input = new JsonXInput(reader, numeric);
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
}
