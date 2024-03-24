package unknow.server.servlet;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import unknow.server.util.io.Buffers.Walker;

public final class Decode implements Walker {
	private final CharsetDecoder d = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
	private final char[] tmp = new char[2048];
	private final CharBuffer cbuf = CharBuffer.wrap(tmp);
	private final ByteBuffer bbuf = ByteBuffer.allocate(4096);
	private final StringBuilder sb;

	public Decode(StringBuilder sb) {
		this.sb = sb;
	}

	private int m = 0;
	private byte pending = 0;

	@Override
	public boolean apply(byte[] b, int o, int e) {
		while (o < e) {
			byte c = b[o++];
			if (m > 0) {
				pending = (byte) ((pending << 4) + (c & 0xff) - '0');
				if (--m == 0) {
					bbuf.put(pending);
					pending = 0;
				}
			} else if (c == '%')
				m = 2;
			else
				bbuf.put(c);
			if (bbuf.remaining() == 0)
				decode();

		}
		return false;
	}

	private void decode() {
		bbuf.flip();
		CoderResult r;
		do {
			r = d.decode(bbuf, cbuf, false);
			cbuf.flip();
			int l = cbuf.length();
			cbuf.get(tmp, 0, l);
			sb.append(tmp, 0, l);
			cbuf.clear();
		} while (r.isOverflow());
		bbuf.compact();
	}

	public boolean done() {
		try {
			if (m != 0)
				return false;
			if (bbuf.position() > 0)
				decode();
			return bbuf.position() == 0;
		} finally {
			cbuf.clear();
			bbuf.clear();
		}
	}
}