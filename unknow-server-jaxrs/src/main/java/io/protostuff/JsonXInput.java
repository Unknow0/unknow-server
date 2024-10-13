package io.protostuff;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.ByteBuffer;

public class JsonXInput implements Input {

	private final PushbackReader r;
	/**
	 * If true, the field number will be used on json keys.
	 */
	private final boolean numeric;

	private final StringBuilder sb;

	private boolean lastRepeated;
	private Schema<?> lastSchema;
	private String lastName;
	private int lastNumber;

	public JsonXInput(Reader r) {
		this(r, false);
	}

	public JsonXInput(Reader r, boolean numeric) {
		this.r = new PushbackReader(r, 4);
		this.numeric = numeric;
		this.sb = new StringBuilder();
	}

	/**
	 * Returns whether the incoming messages' field names are numeric.
	 */
	public boolean isNumeric() {
		return numeric;
	}

	/**
	 * Gets the last field number read.
	 */
	public int getLastNumber() {
		return lastNumber;
	}

	public void readStartObject() throws IOException {
		readNext('{');
	}

	public void readEndObject() throws IOException {
		readNext('}');
	}

	public void readStartArray() throws IOException {
		readNext('[');
	}

	public void readEndArray() throws IOException {
		readNext(']');
	}

	public void reset() {
		lastRepeated = false;
		lastSchema = null;
		lastName = null;
		lastNumber = 0;
	}

	@Override
	public <T> void handleUnknownField(int fieldNumber, Schema<T> schema) throws IOException {
		int nestedObjects = 0;
		while (true) {
			int c = r.read();
			if (c == -1)
				throwEOF();
			if (c == '{' || c == ']')
				nestedObjects++;
			if (c == '}' || c == ']') {
				if (--nestedObjects == 0)
					break;
			}
		}
		readRepeated();
	}

	@Override
	public <T> int readFieldNumber(final Schema<T> schema) throws IOException {
		lastSchema = schema;
		int c;
		if ((c = readNext()) != ',')
			r.unread(c);

		while ((lastNumber = readFieldNumber()) == -1)
			;
		return lastNumber;
	}

	private int readFieldNumber() throws IOException {
		if (lastRepeated) {
			int i = readNext();
			while (readNull(i))
				i = readNext();
			if (i == ']') {
				lastRepeated = false;
				return -1;
			}
			r.unread(i);
			return lastNumber;
		}

		int next = readNext();
		if (next == -1)
			throwEOF();
		if (next == '}')
			return lastNumber = 0;
		if (next != '"')
			throwUnexpectedContent('"', next);
		lastName = readRawString();
		readNext(':');
		next = readNext();
		if (next == -1)
			throwEOF();
		if (next == '[') {
			next = readNext();
			while (readNull(next))
				next = readNext();
			if (next == ']')
				return -1;
			lastRepeated = true;
		}
		if (readNull(next))
			return -1;

		r.unread(next);
		return lastNumber = numeric ? Integer.parseInt(lastName) : lastSchema.getFieldNumber(lastName);
	}

	@Override
	public boolean readBool() throws IOException {
		boolean b = Boolean.parseBoolean(readValue());
		readRepeated();
		return b;
	}

	@Override
	public byte[] readByteArray() throws IOException {
		return B64Code.decode(readString());
	}

	@Override
	public ByteString readBytes() throws IOException {
		return ByteString.wrap(readByteArray());
	}

	@Override
	public void readBytes(final ByteBuffer bb) throws IOException {
		bb.put(readByteArray());
	}

	@Override
	public double readDouble() throws IOException {
		double d = Double.parseDouble(readValue());
		readRepeated();
		return d;
	}

	@Override
	public int readEnum() throws IOException {
		return readInt32();
	}

	@Override
	public int readFixed32() throws IOException {
		String rawValue = readValue();
		readRepeated();
		if (rawValue.startsWith("-"))
			return Integer.parseInt(rawValue);
		return UnsignedNumberUtil.parseUnsignedInt(rawValue);
	}

	@Override
	public long readFixed64() throws IOException {
		String rawValue = readValue();
		readRepeated();
		if (rawValue.startsWith("-")) {
			return Long.parseLong(rawValue);
		}
		return UnsignedNumberUtil.parseUnsignedLong(rawValue);
	}

	@Override
	public float readFloat() throws IOException {
		float value = Float.parseFloat(readValue());
		readRepeated();
		return value;
	}

	@Override
	public int readInt32() throws IOException {
		int value = Integer.parseInt(readValue());
		readRepeated();
		return value;
	}

	@Override
	public long readInt64() throws IOException {
		long value = Long.parseLong(readValue());
		readRepeated();
		return value;
	}

	@Override
	public int readSFixed32() throws IOException {
		return readInt32();
	}

	@Override
	public long readSFixed64() throws IOException {
		return readInt64();
	}

	@Override
	public int readSInt32() throws IOException {
		return readInt32();
	}

	@Override
	public long readSInt64() throws IOException {
		return readInt64();
	}

	@Override
	public String readString() throws IOException {
		readNext('"');
		String value = readRawString();
		readRepeated();
		return value;
	}

	@Override
	public int readUInt32() throws IOException {
		return readFixed32();
	}

	@Override
	public long readUInt64() throws IOException {
		return readFixed64();
	}

	@Override
	public <T> T mergeObject(T value, final Schema<T> schema) throws IOException {
		readStartObject();

		final int lastNumber = this.lastNumber;
		final boolean lastRepeated = this.lastRepeated;
		final String lastName = this.lastName;

		// reset
		this.lastRepeated = false;

		if (value == null)
			value = schema.newMessage();

		schema.mergeFrom(this, value);

		if (!schema.isInitialized(value))
			throw new UninitializedMessageException(value, schema);

		// restore state
		this.lastNumber = lastNumber;
		this.lastRepeated = lastRepeated;
		this.lastName = lastName;

		readRepeated();

		return value;
	}

	@Override
	public void transferByteRangeTo(Output output, boolean utf8String, int fieldNumber, boolean repeated) throws IOException {
		if (utf8String)
			output.writeString(fieldNumber, readString(), repeated);
		else
			output.writeByteArray(fieldNumber, readByteArray(), repeated);
	}

	/**
	 * Reads a byte array/ByteBuffer value.
	 */
	@Override
	public ByteBuffer readByteBuffer() throws IOException {
		return ByteBuffer.wrap(readByteArray());
	}

	/**
	 * read next meaningful char
	 * @return char or -1 on eof
	 * @throws IOException on read error
	 */
	private int readNext() throws IOException {
		int i = r.read();
		while (i == ' ' || i == '\t' || i == '\n' || i == '\r')
			i = r.read();
		return i;
	}

	private boolean readNull(int next) throws IOException {
		if (next != 'n')
			return false;

		int c1 = r.read();
		if (c1 == 'u') {
			int c2 = r.read();
			if (c2 == 'l') {
				int c3 = r.read();
				if (c3 == 'l')
					return true;
				r.unread(c3);
			}
			r.unread(c2);
		}
		r.unread(c1);
		return false;
	}

	private void throwUnexpectedContent(char expected, int actual) throws JsonInputException {
		throwUnexpectedContent(Character.toString(expected), actual == -1 ? "EOF" : Character.toString(actual));
	}

	private void throwUnexpectedContent(String expected, String actual) throws JsonInputException {
		StringBuilder sb = new StringBuilder("Expected ").append(expected).append(" but was ").append(actual).append(" on ").append(lastName);
		if (lastSchema != null)
			sb.append(" of message ").append(lastSchema.messageFullName());
		throw new JsonInputException(sb.toString());
	}

	private void throwEOF() throws JsonInputException {
		throwUnexpectedContent("data", "EOF");
	}

	private void readNext(char expected) throws IOException {
		int i = r.read();
		while (i == ' ' || i == '\t' || i == '\n' || i == '\r')
			i = r.read();

		if (expected != i)
			throwUnexpectedContent(expected, i);
	}

	public boolean isNext(char c) throws IOException {
		int next = readNext();
		r.unread(next);
		return next == c;
	}

	private String readRawString() throws IOException {
		int i;
		sb.setLength(0);
		while ((i = r.read()) != '"') {
			if (i == -1)
				throwEOF();
			if (i == '\\') {
				i = r.read();
				if (i == -1)
					throwEOF();
				if (i == '"' || i == '\\' || i == '/')
					sb.append((char) i);
				else if (i == 'b')
					sb.append('\b');
				else if (i == 'f')
					sb.append('\f');
				else if (i == 'n')
					sb.append('\n');
				else if (i == 'r')
					sb.append('\r');
				else if (i == 't')
					sb.append('\t');
				else if (i == 'u') {
					sb.append((char) (readHex() << 12 | readHex() << 8 | readHex() << 4 | readHex()));
				}
				break;

			}
			sb.append((char) i);
		}
		return sb.toString();
	}

	private int readHex() throws IOException {
		int i = r.read();
		if (i == -1)
			throwEOF();
		if (i >= '0' && i <= '9')
			return i - '0';
		if (i >= 'a' && i <= 'f')
			return 10 + i - 'a';
		if (i >= 'A' && i <= 'F')
			return 10 + i - 'A';
		throwUnexpectedContent("hex digit", Character.toString(i));
		return 0; // will not happen
	}

	private String readValue() throws IOException {
		int i = r.read();
		if (i == '"')
			return readRawString();
		sb.setLength(0);
		while (i != ',' && i != '}' && i != ']') {
			if (i == -1)
				throwEOF();
			sb.append((char) i);
			i = r.read();
		}
		r.unread(i);
		return sb.toString();
	}

	private void readRepeated() throws IOException {
		if (!lastRepeated)
			return;
		int next = readNext();
		if (next == -1)
			throwEOF();

		if (next == ']')
			lastRepeated = false;
	}
}
