package unknow.server.servlet.http2.frame;

import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.http2.Http2Processor;
import unknow.server.util.io.Buffers;

public class FrameSettings extends FrameReader {
	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> {
		if ((flags & 0x1) == 1) {
			if (size != 0)
				p.goaway(Http2Processor.FRAME_SIZE_ERROR);
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

	protected FrameSettings(Http2Processor p, int size, int flags, int id) {
		super(p, size, flags, id);
		b = new byte[9];
	}

	@Override
	public final FrameReader process(Buffers buf) throws InterruptedException {
		while (size > 0 && buf.length() > 6) {
			buf.read(b, 0, 6, false);
			size -= 6;

			int i = (b[0] & 0xff) << 8 | (b[1] & 0xff);
			int v = (b[2] & 0x7f) << 24 | (b[3] & 0xff) << 16 | (b[4] & 0xff) << 8 | (b[5] & 0xff);

			switch (i) {
				case 1:
					synchronized (p.headers) {
						p.headers.setMax(v);
					}
					break;
				case 2:
					if (v < 0 || v > 1) {
						p.goaway(Http2Processor.PROTOCOL_ERROR);
						return null;
					}
//						allowPush = v == 1;
					break;
				case 3:
//						concurrent = v;
					break;
				case 4:
					p.initialWindow = v;
					break;
				case 5:
					if (v < 16384 || v > 16777215) {
						p.goaway(Http2Processor.PROTOCOL_ERROR);
						return null;
					}
					p.frame = v;
					break;
				case 6:
//						headerList = v;
					break;
				default:
					// ignore
			}
		}

		if (size != 0)
			return this;

		Http2Processor.formatFrame(b, 0, 4, 1, 0);

		HttpConnection co = p.co;
		synchronized (co) {
			co.pendingWrite.write(b);
			co.flush();
		}
		return null;
	}
}