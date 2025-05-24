package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.http2.Http2Stream;

public class FrameHeader extends FrameReader {
	private static final Logger logger = LoggerFactory.getLogger(FrameHeader.class);

	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> {
		if (id == 0 || p.streams.contains(id)) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}

		Http2Stream s = new Http2Stream(p, id, p.initialWindow);
		p.addStream(s);
		return new FrameHeader(p, size, flags, id, s).process(buf);
	};

	public static final FrameBuilder CONTINUATION = (p, size, flags, id, buf) -> {
		Http2Stream s = p.streams.get(id);
		if (s == null) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}

		return new FrameHeader(p, size, flags, id, s).process(buf);
	};

	private final Http2Stream s;
	private int pad;
	private int skip;

	protected FrameHeader(Http2Processor p, int size, int flags, int id, Http2Stream s) {
		super(p, size, flags, id);
		this.s = s;
		this.pad = -1;
		this.skip = 5;
	}

	@Override
	public final FrameReader process(ByteBuffer buf) throws IOException {
		if (pad < 0) {
			pad = readPad(buf);
			if (pad < 0)
				return null;
		}

		if ((flags & 0x20) == 1) { // PRIORITY
			int i = Math.min(skip, buf.remaining());
			buf.position(buf.position() + i);
			skip -= i;
			if (skip > 0)
				return this;
			flags ^= 0x20;
		}

		try {
			int l = buf.limit();
			int i = buf.position();
			buf.limit(size - pad + buf.position());
			while (buf.hasRemaining())
				p.headersDecoder.decode(buf, s::addHeader);
			size -= buf.position() - i;
			buf.limit(l);
			if (size > pad)
				return this;

			if (pad > 0) {
				i = Math.min(buf.remaining(), pad);
				buf.position(buf.position() + i);
				if ((pad -= i) > 0)
					return this;
			}

			p.wantContinuation = (flags & 0x4) == 0;
			if (!p.wantContinuation)
				s.start();

			if ((flags & 0x1) == 1) {
				p.streams.remove(id);
				p.pending.set(id, s);
				s.close(false);
			}

			return null;
		} catch (IOException e) {
			logger.error("Failed to parse headers", e);
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}
	}
}