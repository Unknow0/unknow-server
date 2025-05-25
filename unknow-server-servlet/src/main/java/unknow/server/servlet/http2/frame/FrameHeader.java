package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.http2.Http2Stream;

public class FrameHeader extends FrameReader {
	private static final Logger logger = LoggerFactory.getLogger(FrameHeader.class);

	public static final FrameReader INSTANCE = new FrameHeader();

	protected FrameHeader() {
	}

	@Override
	public void check(Http2Processor p, Http2Frame frame) {
		if (frame.id == 0 || p.streams.contains(frame.id)) {
			frame.type = -1;
			p.goaway(Http2Processor.PROTOCOL_ERROR);
		} else
			p.addStream(frame.s = new Http2Stream(p, frame.id, p.initialWindow));

		if ((frame.flags & 0x20) == 1) // PRIORITY
			frame.skip = 5;
		else
			frame.skip = 0;
	}

	@Override
	public void process(Http2Processor p, Http2Frame frame, ByteBuffer buf) throws IOException {
		if (frame.skip > 0) {
			int i = Math.min(frame.skip, buf.remaining());
			buf.position(buf.position() + i);
			if ((frame.skip -= i) > 0 || !buf.hasRemaining())
				return;
		}

		Http2Stream s = frame.s;
		int i = buf.position();
		int l = buf.limit();
		buf.limit(frame.size - frame.pad + buf.position());
		while (buf.hasRemaining()) {
			try {
				p.headersDecoder.decode(buf, s::addHeader);
			} catch (IOException e) {
				logger.error("Failed to parse headers", e);
				buf.limit(l);
				frame.size -= buf.position() - i;
				if (!(p.wantContinuation = (frame.flags & 0x4) == 0))
					frame.s = null;
				p.goaway(Http2Processor.PROTOCOL_ERROR);
				p.streams.remove(s.id());
				frame.type = -1;
				return;
			}
		}
		buf.limit(l);
		if ((frame.size -= buf.position() - i) > frame.pad)
			return;

		if (frame.pad > 0) {
			i = Math.min(buf.remaining(), frame.pad);
			buf.position(buf.position() + i);
			if ((frame.pad -= i) > 0)
				return;
		}

		if (!(p.wantContinuation = (frame.flags & 0x4) == 0)) {
			frame.s = null;
			s.start();
		}

		if ((frame.flags & 0x1) == 1) {
			p.streams.remove(s.id());
			p.pending.set(s.id(), s);
			s.close(false);
		}
	}
}