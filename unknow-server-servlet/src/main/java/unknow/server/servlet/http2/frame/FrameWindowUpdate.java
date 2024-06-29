package unknow.server.servlet.http2.frame;

import unknow.server.servlet.http2.Http2FlowControl;
import unknow.server.servlet.http2.Http2Processor;
import unknow.server.util.io.Buffers;

public class FrameWindowUpdate extends FrameReader {
	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> {
		if (size != 4) {
			p.goaway(Http2Processor.FRAME_SIZE_ERROR);
			return null;
		}
		return new FrameWindowUpdate(p, size, flags, id).process(buf);
	};

	private final byte[] b;

	protected FrameWindowUpdate(Http2Processor p, int size, int flags, int id) {
		super(p, size, flags, id);
		b = new byte[4];
	}

	@Override
	public final FrameReader process(Buffers buf) throws InterruptedException {
		if (buf.length() < 4)
			return this;
		buf.read(b, 0, 4, false);
		int v = (b[0] & 0x7f) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
		if (v == 0) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}

		Http2FlowControl f = id == 0 ? p : p.streams.get(id);
		if (f != null)
			f.add(v);
		return null;
	}
}