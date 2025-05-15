package unknow.server.servlet.http2.frame;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.servlet.http2.Http2Processor;

public class FrameReader {
	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> new FrameReader(p, size, flags, id);

	protected final Http2Processor p;
	protected int size;
	protected int flags;
	protected int id;

	public FrameReader(Http2Processor p, int size, int flags, int id) {
		this.p = p;
		this.size = size;
		this.flags = flags;
		this.id = id;
	}

	/**
	 * read the size of the padding field if the flags is set (and unset it)
	 * 
	 * @param buf where to read
	 * @return the pad length or -1 is case of error
	 * @throws IOException in case of ioexception
	 */
	protected int readPad(ByteBuffer buf) throws IOException {
		if ((flags & 0x8) == 0)
			return 0;
		flags &= ~0x8;

		int pad = buf.get() & 0xFF;
		if (pad >= size) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return -1;
		}
		return pad;
	}

	/**
	 * @param buf the buffer to read
	 * @return this or null
	 * @throws IOException  in case of ioexception
	 */
	public FrameReader process(ByteBuffer buf) throws IOException {
		int r = Math.min(size, buf.remaining());
		buf.position(buf.position() + r);
		size -= r;
		return size == 0 ? null : this;
	}

	public interface FrameBuilder {
		FrameReader build(Http2Processor p, int size, int flags, int id, ByteBuffer buf) throws IOException;
	}
}