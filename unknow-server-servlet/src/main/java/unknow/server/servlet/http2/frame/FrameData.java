package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.http2.Http2Stream;

public class FrameData extends FrameReader {
	public static final FrameReader INSTANCE = new FrameData();

	protected FrameData() {
	}

	@Override
	public void check(Http2Processor p, Http2Frame frame) {
		Http2Stream s = p.streams.get(frame.id);
		if (s == null) {
			frame.type = -1;
			p.goaway(Http2Processor.PROTOCOL_ERROR);
		}
		frame.s = s;
	}

	@Override
	public void process(Http2Processor p, Http2Frame frame, ByteBuffer buf) throws IOException {
		Http2Stream s = frame.s;

		int l = Math.min(buf.remaining(), frame.size - frame.pad);
		s.append(buf.slice().limit(l));
		buf.position(buf.position() + l);
		frame.size -= l;
		if (frame.size - frame.pad > 0)
			return;

		if (frame.pad > 0) {
			l = Math.min(buf.remaining(), frame.pad);
			buf.position(buf.position() + l);
			frame.pad -= l;
			if (frame.pad > 0)
				return;
		}

		if ((frame.flags & 0x1) == 1) {
			p.streams.remove(s.id());
			p.pending.set(s.id(), s);
			s.close(false);
		}
	}

}