package unknow.server.servlet.http2.frame;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.http2.Http2Stream;

public class FrameContinuation extends FrameHeader {
	public static final FrameReader INSTANCE = new FrameContinuation();

	protected FrameContinuation() {
	}

	@Override
	public void check(Http2Processor p, Http2Frame frame) {
		Http2Stream s = p.streams.get(frame.id);
		if (s == null || frame.s != s) {
			frame.s = null;
			frame.type = -1;
			p.goaway(Http2Processor.PROTOCOL_ERROR);
		}
		if ((frame.flags & 0x20) == 1) // PRIORITY
			frame.skip = 5;
		else
			frame.skip = 0;
	}
}