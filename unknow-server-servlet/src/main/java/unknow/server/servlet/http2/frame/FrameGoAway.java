package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;

public class FrameGoAway extends FrameReader {
	private static final Logger logger = LoggerFactory.getLogger(FrameGoAway.class);

	public static final FrameReader INSTANCE = new FrameGoAway();

	protected FrameGoAway() {
	}

	@Override
	public void check(Http2Processor p, Http2Frame frame) {
		if (frame.id != 0)
			p.goaway(Http2Processor.PROTOCOL_ERROR);
		frame.lastId = -1;
	};

	@Override
	public void process(Http2Processor p, Http2Frame frame, ByteBuffer buf) throws IOException {
		if (frame.lastId >= 0) {
			super.process(p, frame, buf);
			return;
		}

		byte[] b = frame.b;

		int i = Math.min(buf.remaining(), 8 - frame.l);
		buf.get(b, frame.l, i);
		if ((frame.l += i) < 8)
			return;
		frame.l = 0;

		frame.lastId = (b[0] & 0x7f) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);

		int err = (b[4] & 0xff) << 24 | (b[5] & 0xff) << 16 | (b[6] & 0xff) << 8 | (b[7] & 0xff);
		logger.info("goaway last: {} err: {}", frame.lastId, Http2Processor.error(err));
		frame.size -= 8;
		p.close(frame.lastId);

		int r = Math.min(frame.size, buf.remaining());
		logger.info("	dbg: {}", new String(buf.array(), buf.position(), r));
		buf.position(buf.position() + r);
		frame.size -= r;
	}
}