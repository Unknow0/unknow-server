package unknow.server.servlet.http2.frame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.http2.Http2Stream;
import unknow.server.util.io.Buffers;

public class FrameRstStream extends FrameReader {
	private static final Logger logger = LoggerFactory.getLogger(FrameRstStream.class);

	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> {
		Http2Stream s = p.streams.remove(id);
		if (s == null) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}
		if (size != 4) {
			p.goaway(Http2Processor.FRAME_SIZE_ERROR);
			return null;
		}

		return new FrameRstStream(p, size, flags, id, s).process(buf);
	};

	private final Http2Stream s;
	private final byte[] b;

	protected FrameRstStream(Http2Processor p, int size, int flags, int id, Http2Stream s) {
		super(p, size, flags, id);
		this.s = s;
		this.b = new byte[4];
	}

	@Override
	public final FrameReader process(Buffers buf) throws InterruptedException {
		if (buf.length() < 4)
			return null;
		buf.read(b, 0, 4, false);
		int err = (b[0] & 0xff) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
		logger.info("closing stream {} err: {}", id, Http2Processor.error(err));
		s.close(true);
		return null;
	}
}