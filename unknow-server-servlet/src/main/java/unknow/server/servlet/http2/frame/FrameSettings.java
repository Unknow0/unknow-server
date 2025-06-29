package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;

public class FrameSettings extends FrameReader {
	private static final Logger logger = LoggerFactory.getLogger(FrameSettings.class);

	public static final FrameReader INSTANCE = new FrameSettings();

	protected FrameSettings() {
	}

	@Override
	public void check(Http2Processor p, Http2Frame frame) {
		if ((frame.flags & 0x1) == 1) {
			if (frame.size != 0)
				p.goaway(Http2Processor.FRAME_SIZE_ERROR);
			logger.trace("{}: settings ack", p);
			frame.type = -1;
			return;
		}
		if (frame.id != 0)
			p.goaway(Http2Processor.PROTOCOL_ERROR);
		if (frame.size % 6 != 0)
			p.goaway(Http2Processor.FRAME_SIZE_ERROR);
	}

	@Override
	public void process(Http2Processor p, Http2Frame frame, ByteBuffer buf) throws IOException {
		byte[] b = frame.b;
		while (frame.size > 0 && buf.hasRemaining()) {
			int i = Math.min(buf.remaining(), 6 - frame.l);
			buf.get(b, frame.l, i);
			frame.size -= i;
			if ((frame.l += i) < 6)
				return;
			frame.l = 0;

			i = (b[0] & 0xff) << 8 | (b[1] & 0xff);
			int v = (b[2] & 0x7f) << 24 | (b[3] & 0xff) << 16 | (b[4] & 0xff) << 8 | (b[5] & 0xff);

			switch (i) {
				case 1:
					logger.trace("{}: SETTINGS_HEADER_TABLE_SIZE  {}", p, v);
					synchronized (p.headersEncoder) {
						p.headersEncoder.setMax(v);
					}
					break;
				case 2:
					logger.trace("{}: SETTINGS_ENABLE_PUSH {}", p, v);
					if (v < 0 || v > 1)
						p.goaway(Http2Processor.PROTOCOL_ERROR);
					break;
				case 3:
					logger.trace("{}: SETTINGS_MAX_CONCURRENT_STREAMS {}", p, v);
					break;
				case 4:
					logger.trace("{}: SETTINGS_INITIAL_WINDOW_SIZE {}", p, v);
					p.initialWindow = v;
					break;
				case 5:
					logger.trace("{}: SETTINGS_MAX_FRAME_SIZE {}", p, v);
					if (v < 16384 || v > 16777215)
						p.goaway(Http2Processor.PROTOCOL_ERROR);
					else
						p.frameSize = v;
					break;
				case 6:
					logger.trace("{}: SETTINGS_MAX_HEADER_LIST_SIZE {}", p, v);
					break;
				default:
					// ignore
			}
		}

		p.sendFrame(4, 1, 0, null);
	}
}