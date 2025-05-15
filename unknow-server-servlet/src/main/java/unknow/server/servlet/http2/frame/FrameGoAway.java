package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;

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
	private int l;

	private int lastId = -1;

	protected FrameGoAway(Http2Processor p, int size, int flags, int id) {
		super(p, size, flags, id);
		b = new byte[8];
	}

	@Override
	public FrameReader process(ByteBuffer buf) throws IOException {
		if (lastId >= 0)
			return super.process(buf);

		int i = Math.min(buf.remaining(), 8 - l);
		buf.get(b, l, i);
		if ((l += i) < 8)
			return this;

		lastId = (b[0] & 0x7f) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);

		int err = (b[4] & 0xff) << 24 | (b[5] & 0xff) << 16 | (b[6] & 0xff) << 8 | (b[7] & 0xff);
		logger.info("goaway last: {} err: {}", lastId, Http2Processor.error(err));
		size -= 8;
		p.close(lastId);
		return super.process(buf);
	}
}