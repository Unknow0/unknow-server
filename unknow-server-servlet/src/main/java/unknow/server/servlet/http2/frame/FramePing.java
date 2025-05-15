package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.servlet.http2.Http2Processor;

public class FramePing extends FrameReader {
	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> {
		if (id != 0) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}
		if (size != 8) {
			p.goaway(Http2Processor.FRAME_SIZE_ERROR);
			return null;
		}
		return new FramePing(p, size, flags, id).process(buf);
	};

	private final byte[] b;
	private int l;

	protected FramePing(Http2Processor p, int size, int flags, int id) {
		super(p, size, flags, id);
		this.b = new byte[8];
		this.l = 0;
	}

	@Override
	public final FrameReader process(ByteBuffer buf) throws IOException {
		int i = Math.min(8 - l, buf.remaining());
		buf.get(b, l, i);
		if ((l += i) < 8)
			return this;

		if ((flags & 0x1) == 0)
			p.sendFrame(8, 1, 0, ByteBuffer.wrap(b));
		return null;
	}
}