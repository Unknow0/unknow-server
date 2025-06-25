package unknow.server.servlet.http2.header;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

import unknow.server.servlet.http2.header.Http2Huffman.S;

public class Http2HeadersDecoder extends Http2Headers {

	private enum State {
		READ_HEADER_FIRST, READ_INT_CONT, READ_NAME_LEN, READ_STRLEN, READ_NAME, READ_VALUE_LEN, READ_VALUE
	}

	private final StringBuilder sb;
	private final S s;

	private State state = State.READ_HEADER_FIRST;
	private State nextState;
	private int type;
	private int value;
	private int shift;
	private boolean huffman;
	private String name;

	public Http2HeadersDecoder(int maxSize) {
		super(maxSize);
		sb = new StringBuilder();
		s = new S();
	}

	/**
	* read all headers
	* @param chunk the input
	* @param h the produced header key/value
	* @throws IOException in case of error
	*/
	public void decode(ByteBuffer chunk, BiConsumer<String, String> h) throws IOException {
		while (chunk.hasRemaining())
			process(chunk, h);
	}

	private void process(ByteBuffer chunk, BiConsumer<String, String> h) throws IOException {
		int bb;
		switch (state) {
			case READ_HEADER_FIRST:
				int b = chunk.get() & 0xFF;
				int prefix;
				if ((b & 0b1000_0000) != 0) {      // indexed
					type = 1;
					prefix = 7;
				} else if ((b & 0b0100_0000) != 0) { // literal + indexing
					type = 2;
					prefix = 6;
				} else if ((b & 0b0010_0000) != 0) { // table size update
					type = 3;
					prefix = 5;
				} else {                             // literal no indexing
					type = 4;
					prefix = 4;
				}
				value = b & MASK[prefix];
				if (value == MAX[prefix]) { // on continue l'entier
					shift = 0;
					state = State.READ_INT_CONT;
				} else
					onIntegerDecoded(h);
				break;
			case READ_INT_CONT:
				bb = chunk.get() & 0xFF;
				value |= (bb & 0x7F) << shift;
				shift += 7;
				if ((bb & 0x80) == 0)
					onIntegerDecoded(h);
				break;
			case READ_NAME_LEN:
				bb = chunk.get() & 0xFF;
				huffman = (bb & 0x80) != 0;
				s.max = bb & 0x7F;
				if (s.max == 0x7F) {
					nextState = State.READ_NAME;
					state = State.READ_STRLEN;
					shift = 0;
				} else
					state = State.READ_NAME;
				break;
			case READ_STRLEN:
				bb = chunk.get() & 0xFF;
				s.max |= (bb & 0x7F) << shift;
				shift += 7;
				if ((bb & 0x80) == 0)
					state = nextState;
				break;
			case READ_NAME:
				if (decodeString(chunk)) {
					name = sb.toString();
					sb.setLength(0);
					state = State.READ_VALUE_LEN;
				}
				break;
			case READ_VALUE_LEN:
				int vb = chunk.get() & 0xFF;
				huffman = (vb & 0x80) != 0;
				s.max = vb & 0x7F;
				if (s.max == 0x7F) {
					nextState = State.READ_VALUE;
					state = State.READ_STRLEN;
					break;
				}
				state = State.READ_VALUE; // fallthrough
			case READ_VALUE:
				if (decodeString(chunk)) {
					String str = sb.toString();
					h.accept(name, str);
					sb.setLength(0);
					state = State.READ_HEADER_FIRST;
					if (type == 2)
						add(new Entry(name, str));
				}
				break;
			default:
		}
	}

	private boolean decodeString(ByteBuffer b) throws IOException {
		if (huffman) {
			Http2Huffman.decode(b, s, sb);
			if (s.max <= 0) {
				s.cnt = s.bit = 0;
				return true;
			}
			return false;
		}
		int l = Math.min(b.remaining(), s.max);
		for (int i = 0; i < l; i++)
			sb.append((char) (b.get() & 0xFF));
		return (s.max -= l) == 0;
	}

	private void onIntegerDecoded(BiConsumer<String, String> handler) throws IOException {
		switch (type) {
			case 1: // indexed
				if (value == 0)
					throw new IOException("invalid index");
				Entry e = get(value);
				handler.accept(e.name(), e.value());
				state = State.READ_HEADER_FIRST;
				break;
			case 2: // literal with indexing
			case 4: // literal without indexing
				if (value > 0) {
					name = get(value).name();
					state = State.READ_VALUE_LEN;
				} else
					state = State.READ_NAME_LEN;
				break;
			case 3: // update size
				if (value > settingsSize)
					throw new IOException("Update size > max size");
				maxSize = value;
				ensureMax();
				state = State.READ_HEADER_FIRST;
				break;
		}
	}
}