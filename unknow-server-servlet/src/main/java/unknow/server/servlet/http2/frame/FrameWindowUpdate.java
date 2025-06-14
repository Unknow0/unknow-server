package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.servlet.http2.Http2FlowControl;
import unknow.server.servlet.http2.Http2Processor;

public class FrameWindowUpdate extends FrameReader {
	public static final FrameReader INSTANCE = new FrameWindowUpdate();

	protected FrameWindowUpdate() {
	}

	@Override
	public void check(Http2Processor p, Http2Frame frame) {
		if (frame.size != 4) {
			p.goaway(Http2Processor.FRAME_SIZE_ERROR);
			frame.type = -1;
		}
	}

	@Override
	public void process(Http2Processor p, Http2Frame frame, ByteBuffer buf) throws IOException {
		byte[] b = frame.b;
		int i = Math.min(buf.remaining(), 4 - frame.l);
		buf.get(b, frame.l, i);
		if ((frame.l += i) < 4)
			return;
		frame.l = 0;
		frame.size = 0;

		int v = (b[0] & 0x7f) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
		if (v == 0) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			frame.type = -1;
			return;
		}

		Http2FlowControl f = frame.id == 0 ? p : p.streams.get(frame.id);
		if (f != null)
			f.flowWrite(-v);
	}
}