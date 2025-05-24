package unknow.server.servlet.http2.frame;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.http2.Http2Stream;

public class FrameRstStream extends FrameReader {
	private static final Logger logger = LoggerFactory.getLogger(FrameRstStream.class);

	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> {
		if (id == 0) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}
		if (size != 4) {
			p.goaway(Http2Processor.FRAME_SIZE_ERROR);
			return null;
		}

		return new FrameRstStream(p, size, flags, id).process(buf);
	};

	private final byte[] b;
	private int l;

	protected FrameRstStream(Http2Processor p, int size, int flags, int id) {
		super(p, size, flags, id);
		this.b = new byte[4];
		this.l = 0;
	}

	@Override
	public final FrameReader process(ByteBuffer buf) {
		int i = Math.min(4 - l, buf.remaining());
		buf.get(b, l, i);
		if ((l += i) < 4)
			return this;

		int err = (b[0] & 0xff) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
		logger.info("closing stream {} err: {}", id, Http2Processor.error(err));
		Http2Stream s = p.getStream(id);
		if (s != null)
			s.close(true);
		return null;
	}
}