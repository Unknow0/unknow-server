package unknow.server.servlet.http11;

import java.nio.ByteBuffer;

import jakarta.servlet.DispatcherType;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.utils.Utf8Decoder;

public class RequestDecoder {
	private static final byte[] CRLF = { '\r', '\n' };
	private static final byte SPACE = ' ';

	private enum State {
		METHOD, URI, PROTOCOL, HEADER, DONE
	}

	private final Http11Processor co;
	private final Utf8Decoder decoder;
	private final byte[] buf;

	private ServletRequestImpl req;
	private State state;
	private int f;
	private int o;
	private int l;

	public RequestDecoder(Http11Processor co) {
		this.co = co;
		decoder = new Utf8Decoder(new StringBuilder());
		buf = new byte[4096];
		reset();
	}

	public void addContent(ByteBuffer b) {
		req.append(b);
	}

	public void closeContent() {
		req.close();
	}

	public boolean closed() {
		return req.isClosed();
	}

	public ServletRequestImpl append(ByteBuffer b) {
		while (b.hasRemaining()) {
			int s = b.position();
			l = Math.min(b.remaining(), buf.length);
			b.get(buf, 0, l);
			o = 0;
			while (o < l) {
				tryDecode();
				if (state == State.DONE) {
					b.position(s + o);
					return req;
				}
			}
		}
		return null;
	}

	private void tryDecode() {
		String str;
		switch (state) {
			case METHOD:
				if ((str = readUntil(SPACE)) != null) {
					state = State.URI;
					req.setMethod(str);
				}
				return;
			case URI:
				if ((str = readUntil(SPACE)) != null) {
					state = State.PROTOCOL;
					int i = str.indexOf('?');
					if (i > 0) {
						req.setQuery(str.substring(i + 1));
						str = str.substring(0, i);
					}
					req.setRequestUri(str);
				}
				return;
			case PROTOCOL:
				if ((str = readUntil(CRLF)) != null) {
					state = State.HEADER;
					req.setProtocol(str);
				}
				return;
			case HEADER:
				if ((str = readUntil(CRLF)) != null) {
					if (str.isEmpty()) {
						state = State.DONE;
						return;
					}
					int i = str.indexOf(':');
					if (i < 0)
						throw new IllegalArgumentException("invalid header line " + str);
					req.addHeader(str.substring(0, i).trim().toLowerCase(), str.substring(i + 1).trim());
				}
				return;
			case DONE:
				return;
		}
	}

	private String readUntil(byte c) {
		int i = o;
		while (i < l) {
			if (buf[i] == c) {
				String str = decoder.append(buf, o, i).done();
				o = i + 1;
				return str;
			}
			i++;
		}
		decoder.append(buf, o, l);
		o = l;
		return null;
	}

	private String readUntil(byte[] c) {
		int i = o;
		while (i < l) {
			byte t = buf[i++];
			if (c[f++] != t) {
				decoder.append(c, 0, f - 1);
				f = 0;
			} else if (f == c.length) {
				if (i > c.length)
					decoder.append(buf, o, i - c.length);
				f = 0;
				o = i;
				return decoder.done();
			}
		}
		decoder.append(buf, o, l - f);
		o = l;
		return null;
	}

	public void reset() {
		state = State.METHOD;
		f = 0;
		req = new ServletRequestImpl(co.co(), DispatcherType.REQUEST);
	}
}
