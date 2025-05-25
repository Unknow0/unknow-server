package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import unknow.server.servlet.http2.Http2Processor;

public class FramePing extends FrameReader {
	public static final FrameReader INSTANCE = new FramePing();

	protected FramePing() {
	}

	@Override
	public void check(Http2Processor p, Http2Frame frame) {
		if (frame.id != 0) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			frame.type = -1;
		} else if (frame.size != 8) {
			p.goaway(Http2Processor.FRAME_SIZE_ERROR);
			frame.type = -1;
		}
	};

	@Override
	public void process(Http2Processor p, Http2Frame frame, ByteBuffer buf) throws IOException {
		if (frame.id != 0 || frame.size != 8)
			super.process(p, frame, buf);

		int i = Math.min(8 - frame.l, buf.remaining());
		buf.get(frame.b, frame.l, i);
		if ((frame.l += i) < 8)
			return;
		frame.l = 0;
		frame.size = 0;

		if ((frame.flags & 0x1) == 0)
			p.sendFrame(8, 1, 0, ByteBuffer.wrap(Arrays.copyOf(frame.b, 8)));
	}
}