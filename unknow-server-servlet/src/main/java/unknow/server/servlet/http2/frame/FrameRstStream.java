package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;

public class FrameRstStream extends FrameReader {
	private static final Logger logger = LoggerFactory.getLogger(FrameRstStream.class);

	public static final FrameReader INSTANCE = new FrameRstStream();

	protected FrameRstStream() {
	}

	@Override
	public void check(Http2Processor p, Http2Frame frame) {
		if (frame.id == 0) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			frame.type = -1;
		} else if (frame.size != 4) {
			p.goaway(Http2Processor.FRAME_SIZE_ERROR);
			frame.type = -1;
		}
	}

	@Override
	public void process(Http2Processor p, Http2Frame frame, ByteBuffer buf) throws IOException {
		byte[] b = frame.b;
		int i = Math.min(4 - frame.l, buf.remaining());
		buf.get(b, frame.l, i);
		if ((frame.l += i) < 4)
			return;
		frame.l = 0;
		frame.size = 0;

		int err = (b[0] & 0xff) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
		logger.debug("closing stream {} err: {}", frame.id, Http2Processor.error(err));
		p.closeStream(frame.id);
	}
}