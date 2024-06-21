package unknow.server.servlet.http2.frame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.util.io.Buffers;

public class FrameGoAway extends FrameReader {
	private static final Logger logger = LoggerFactory.getLogger(FrameGoAway.class);

	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> {
		if (id != 0) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}
		return new FrameGoAway(p, size, flags, id).process(buf);
	};

	private final byte[] b;

	private int lastId = -1;

	protected FrameGoAway(Http2Processor p, int size, int flags, int id) {
		super(p, size, flags, id);
		b = new byte[4];
	}

	@Override
	public FrameReader process(Buffers buf) throws InterruptedException {
		if (lastId >= 0)
			return super.process(buf);
		if (buf.length() < 8)
			return this;

		buf.read(b, 0, 4, false);
		lastId = (b[0] & 0x7f) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);

		buf.read(b, 0, 4, false);
		int err = (b[0] & 0x7f) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
		logger.info("goaway last: {} err: {}", lastId, Http2Processor.error(err));
		size -= 8;
		p.closing = true;
		return super.process(buf);
	}
}