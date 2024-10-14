package io.protostuff;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class JsonXInput implements Input {
	private static final byte[] NULL = { 'n', 'u', 'l', 'l' };
	private static final byte BEL = '\b';
	private static final byte TAB = '\t';
	private static final byte FF = '\f';
	private static final byte LF = '\n';
	private static final byte CR = '\r';
	private final InputStream in;
	private final boolean numeric;

	private final Utf8Decoder sb;
	private final byte[] buf;
	private int o;
	private int l;

	private boolean lastRepeated;
	private Schema<?> lastSchema;
	private String lastName;
	private int lastNumber;

	public JsonXInput(InputStream in) {
		this(in, false);
	}

	public JsonXInput(InputStream in, boolean numeric) {
		this.in = in;
		this.numeric = numeric;
		this.sb = new Utf8Decoder();
		this.buf = new byte[4096];
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
			checkBuffer(1);
			byte b = buf[o++];
			if (b == '{' || b == ']')
				nestedObjects++;
			if (b == '}' || b == ']') {
				if (--nestedObjects == 0)
					break;
			}
		}
		readRepeated();
	}

	@Override
	public <T> int readFieldNumber(final Schema<T> schema) throws IOException {
		lastSchema = schema;
		if (readNext() == ',')
			o++;

		while ((lastNumber = readFieldNumber()) == -1)
			;
		return lastNumber;
	}

	private int readFieldNumber() throws IOException {
		if (lastRepeated) {
			int i = readNext();
			while (readNull())
				i = readNext();
			if (i == ']') {
				o++;
				lastRepeated = false;
				return -1;
			}
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
			while (readNull())
				next = readNext();
			if (next == ']')
				return -1;
			lastRepeated = true;
		}
		if (readNull())
			return -1;

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

	private void checkBuffer(int n) throws IOException {
		if (o + n < l)
			return;
		if (o != l) {
			l -= o;
			System.arraycopy(buf, o, buf, o, l);
			o = 0;
		} else
			o = l = 0;
		do {
			int i = in.read(buf, o, buf.length - l);
			if (i == -1)
				throwEOF();
			l += i;
		} while (o + n < l);
	}

	/**
	 * read next meaningful char
	 * @return char or -1 on eof
	 * @throws IOException on read error
	 */
	private int readNext() throws IOException {
		checkBuffer(1);
		byte b = buf[o];
		while (b == ' ' || b == '\t' || b == '\n' || b == '\r') {
			b = buf[++o];
			checkBuffer(1);
		}
		return b;
	}

	private boolean readNull() throws IOException {
		checkBuffer(4);
		if (l - o > 4 && Arrays.equals(buf, o, o + 4, NULL, 0, 4)) {
			o += 4;
			return true;
		}
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

		byte b = buf[o];
		while (b == ' ' || b == '\t' || b == '\n' || b == '\r')
			b = buf[++o];

		if (expected != b)
			throwUnexpectedContent(expected, b);
	}

	public boolean isNext(char c) throws IOException {
		return readNext() == c;
	}

	private String readRawString() throws IOException {
		byte i;
		while ((i = buf[o++]) != '"') {
			checkBuffer(1);
			if (i == '\\') {
				i = buf[o++];
				if (i == '"' || i == '\\' || i == '/')
					sb.append(i);
				else if (i == 'b')
					sb.append(BEL);
				else if (i == 'f')
					sb.append(FF);
				else if (i == 'n')
					sb.append(LF);
				else if (i == 'r')
					sb.append(CR);
				else if (i == 't')
					sb.append(TAB);
				else if (i == 'u') {
					checkBuffer(4);
					sb.appendUnicode(readHex() << 12 | readHex() << 8 | readHex() << 4 | readHex());
				}
				break;
			}
			sb.append(i);
		}
		return sb.done();
	}

	private int readHex() throws IOException {
		byte b = buf[o++];
		if (b >= '0' && b <= '9')
			return b - '0';
		if (b >= 'a' && b <= 'f')
			return 10 + b - 'a';
		if (b >= 'A' && b <= 'F')
			return 10 + b - 'A';
		throwUnexpectedContent("hex digit", Character.toString(b));
		return 0; // will not happen
	}

	private String readValue() throws IOException {
		byte i = buf[o++];
		if (i == '"')
			return readRawString();
		while (i != ',' && i != '}' && i != ']') {
			sb.append(i);
			checkBuffer(1);
			i = buf[o++];
		}
		o--;
		return sb.done();
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
