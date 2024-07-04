package unknow.server.servlet.http2.frame;

import unknow.server.util.io.Buffers;

public class FramePRI extends FrameReader {
	public static final FramePRI INSTRANCE = new FramePRI();

	private FramePRI() {
		super(null, 0, 0, 0);
	}

	@Override
	public FrameReader process(Buffers buf) throws InterruptedException {
		if (buf.length() < 24)
			return this;
		buf.skip(24);
		return null;
	}
}
