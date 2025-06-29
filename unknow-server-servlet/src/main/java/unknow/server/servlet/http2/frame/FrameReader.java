package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.servlet.http2.Http2Processor;

public class FrameReader {
	public static final FrameReader INSTANCE = new FrameReader();

	protected FrameReader() {
	}

	public void check(@SuppressWarnings("unused") Http2Processor p, @SuppressWarnings("unused") Http2Frame frame) { // ok
	}

	/**
	 * @param p the processor
	 * @param frame http2frame
	 * @param buf the buffer to read
	 * @throws IOException  in case of ioexception
	 */
	public void process(@SuppressWarnings("unused") Http2Processor p, Http2Frame frame, ByteBuffer buf) throws IOException {
		int r = Math.min(frame.size, buf.remaining());
		buf.position(buf.position() + r);
		frame.size -= r;
	}
}