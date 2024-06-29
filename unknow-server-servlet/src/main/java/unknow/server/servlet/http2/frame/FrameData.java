package unknow.server.servlet.http2.frame;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.http2.Http2Stream;
import unknow.server.util.io.Buffers;

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
	public FrameReader process(Buffers buf) throws InterruptedException {
		if (pad < 0) {
			pad = readPad(buf);
			if (pad < 0)
				return null;
			size -= pad;
		}

		int l = Math.min(buf.length(), size);
		s.in.read(buf, l);
		size -= l;
		if (size > 0)
			return this;

		if (pad > 0) {
			pad -= buf.skip(pad);
			if (pad > 0)
				return this;
		}

		if ((flags & 0x1) == 1) {
			p.streams.remove(id);
			p.pending.add(s);
			s.close(false);
		}
		return null;
	}

}