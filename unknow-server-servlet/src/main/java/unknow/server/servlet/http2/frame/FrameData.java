package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.http2.Http2Stream;

public class FrameData extends FrameReader {
	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> {
		Http2Stream s = p.streams.get(id);
		if (s == null) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}

		return new FrameData(p, size, flags, id).process(buf);
	};

	private final Http2Stream s;
	private int pad;

	protected FrameData(Http2Processor p, int size, int flags, int id) {
		super(p, size, flags, id);
		this.s = p.streams.get(id);
		this.pad = -1;
	}

	@Override
	public FrameReader process(ByteBuffer buf) throws IOException {
		if (pad < 0) {
			pad = readPad(buf);
			if (pad < 0)
				return null;
			size -= pad;
		}

		int l = Math.min(buf.remaining(), size);
		s.append(buf.slice().limit(l));
		buf.position(buf.position() + l);
		size -= l;
		if (size > 0)
			return this;

		if (pad > 0) {
			l = Math.min(buf.remaining(), pad);
			buf.position(buf.position() + l);
			pad -= l;
			if (pad > 0)
				return this;
		}

		if ((flags & 0x1) == 1) {
			p.streams.remove(id);
			p.pending.set(id, s);
			s.close(false);
		}
		return null;
	}

}