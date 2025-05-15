package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;

public class FrameSettings extends FrameReader {
	private static final Logger logger = LoggerFactory.getLogger(FrameSettings.class);
	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> {
		if ((flags & 0x1) == 1) {
			if (size != 0)
				p.goaway(Http2Processor.FRAME_SIZE_ERROR);
			logger.trace("{}: settings ack", p);
			return null;
		}
		if (id != 0) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}
		if (size % 6 != 0) {
			p.goaway(Http2Processor.FRAME_SIZE_ERROR);
			return null;
		}
		return new FrameSettings(p, size, flags, id).process(buf);
	};

	private final byte[] b;
	private int l;

	protected FrameSettings(Http2Processor p, int size, int flags, int id) {
		super(p, size, flags, id);
		b = new byte[9];
		l = 0;
	}

	@Override
	public final FrameReader process(ByteBuffer buf) throws IOException {
		while (size > 0) {
			int i = Math.min(buf.remaining(), 6 - l);
			buf.get(b, l, i);
			size -= i;
			if ((l += i) < 6)
				return this;
			l = 0;

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
					if (v < 0 || v > 1) {
						p.goaway(Http2Processor.PROTOCOL_ERROR);
						return null;
					}
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
					if (v < 16384 || v > 16777215) {
						p.goaway(Http2Processor.PROTOCOL_ERROR);
						return null;
					}
					p.frame = v;
					break;
				case 6:
					logger.trace("{}: SETTINGS_MAX_HEADER_LIST_SIZE {}", p, v);
					break;
				default:
					// ignore
			}
		}

		p.sendFrame(4, 1, 0, null);
		return null;
	}
}