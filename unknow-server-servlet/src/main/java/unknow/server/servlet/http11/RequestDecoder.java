package unknow.server.servlet.http11;

import java.nio.ByteBuffer;

import jakarta.servlet.DispatcherType;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.utils.Utf8Decoder;

public class RequestDecoder {
	private static final byte SPACE = ' ';
	private static final byte CR = '\r';
	private static final byte LF = '\n';

	private enum State {
		METHOD, URI, PROTOCOL, HEADER, DONE
	}

	private final Http11Processor co;
	private final Utf8Decoder decoder;
	private final byte[] buf;

	private ServletRequestImpl req;
	private State state;
	private boolean cr;
	private int o;
	private int l;

	public RequestDecoder(Http11Processor co) {
		this.co = co;
		decoder = new Utf8Decoder();
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
				if ((str = readUntilCrlf()) != null) {
					state = State.HEADER;
					req.setProtocol(str);
				}
				return;
			case HEADER:
				if ((str = readUntilCrlf()) != null) {
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

	private String readUntilCrlf() {
		int i = o;
		if (i == l)
			return null;
		if (cr) {
			cr = false;
			if (buf[i] == LF) {
				o++;
				return decoder.done();
			}
			decoder.append(CR);
		}

		while (i < l) {
			byte t = buf[i++];
			if (t == CR) {
				if (i == l) {
					cr = true;
					decoder.append(buf, o, l - 1);
					o = l;
					return null;
				}
				if (buf[i] == LF) {
					String str = decoder.append(buf, o, i - 1).done();
					o = i + 1;
					return str;
				}
			}
		}
		decoder.append(buf, o, l);
		o = l;
		return null;
	}

	public void reset() {
		state = State.METHOD;
		cr = false;
		req = new ServletRequestImpl(co.co(), DispatcherType.REQUEST);
	}
}
