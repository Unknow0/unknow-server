package unknow.server.servlet.http11;

import java.nio.ByteBuffer;
import java.util.Arrays;

import jakarta.servlet.DispatcherType;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.utils.Utf8Decoder;

public class RequestDecoder {
	private static final byte[] CRLF = { '\r', '\n' };
	private static final byte SPACE = ' ';

	private static enum State {
		METHOD, URI, PROTOCOL, HEADER, DONE
	}

	private final Http11Processor co;
	private final StringBuilder sb;
	private final Utf8Decoder decoder;

	private ServletRequestImpl req;
	private State state;
	private int f;

	public RequestDecoder(Http11Processor co) {
		this.co = co;
		sb = new StringBuilder();
		decoder = new Utf8Decoder(sb, false);
		reset();
	}

	public ServletRequestImpl append(ByteBuffer b) {
		while (b.hasRemaining()) {
			tryDecode(b);
			if (state == State.DONE)
				return req;
		}
		return null;
	}

	private void tryDecode(ByteBuffer b) {
		String str;
		switch (state) {
			case METHOD:
				str = readUntil(b, SPACE);
				if (str != null) {
					state = State.URI;
					req.setMethod(str);
				}
				break;
			case URI:
				str = readUntil(b, SPACE);
				if (str != null) {
					state = State.PROTOCOL;
					int i = str.indexOf('?');
					if (i > 0) {
						req.setQuery(str.substring(i + 1));
						str = str.substring(0, i);
					}
					req.setRequestUri(str);
				}
				break;
			case PROTOCOL:
				str = readUntil(b, CRLF);
				if (str != null) {
					state = State.HEADER;
					req.setProtocol(str);
				}
				break;
			case HEADER:
				str = readUntil(b, CRLF);
				if (str != null) {
					if (str.isEmpty())
						state = State.DONE;
					else {
						int i = str.indexOf(':');
						if (i < 0)
							throw new IllegalArgumentException("invalid header line " + str);
						req.addHeader(str.substring(0, i).trim().toLowerCase(), str.substring(i + 1).trim());
					}
				}
			case DONE:
				return;
		}
	}

	private String readUntil(ByteBuffer b, byte c) {
		byte[] a = b.array();
		int o = b.position();
		int l = b.limit();

		int i = o;
		while (i < l) {
			if (a[i] == c) {
				String str = decoder.append(a, o, i).done();
				b.position(i + 1);
				return str;
			}
			i++;
		}
		decoder.append(a, o, l);
		b.position(l);
		return null;
	}

	private String readUntil(ByteBuffer b, byte[] c) {
		byte[] a = b.array();
		int o = b.position();
		int l = b.limit();

		if (f > 0) {
			int i = o;
			while (i < l) {
				byte t = a[i++];
				if (c[f++] != t) {
					decoder.append(c, 0, f - 1);
					f = 0;
					break;
				} else if (f == c.length) {
					f = 0;
					b.position(i);
					return decoder.done();
				}
			}
		}

		int e = l - c.length + 1;
		int i = o;
		while (i < e) {
			if (Arrays.equals(a, i, i + c.length, c, 0, c.length)) {
				String str = decoder.append(a, o, i).done();
				b.position(i + c.length);
				return str;
			}
			i++;
		}
		decoder.append(a, o, e);
		while (i < l) {
			byte t = a[i++];
			if (c[f] == t)
				f++;
			else {
				if (f > 0) {
					decoder.append(c, 0, f);
					f = 0;
				}
				decoder.append(t);
			}
		}

		b.position(l);
		return null;
	}

	public void reset() {
		state = State.METHOD;
		req = new Http11Request(co, DispatcherType.REQUEST);
	}
}
