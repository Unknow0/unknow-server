package unknow.server.servlet.http2.frame;

import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.http2.Http2Processor;
import unknow.server.util.io.Buffers;

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
		if ((flags & 0x1) == 1)
			return null;
		return new FramePing(p, size, flags, id).process(buf);
	};

	private final byte[] b;

	protected FramePing(Http2Processor p, int size, int flags, int id) {
		super(p, size, flags, id);
		b = new byte[9 + 8];

	}

	@Override
	public final FrameReader process(Buffers buf) throws InterruptedException {
		if (buf.length() < 8)
			return this;

		buf.read(b, 9, 8, false);
		Http2Processor.formatFrame(b, 8, 8, 1, 0);
		HttpConnection co = p.co;
		Buffers write = co.pendingWrite;
		write.lock();
		try {
			write.write(b);
			co.flush();
		} finally {
			write.unlock();
		}
		return null;
	}
}