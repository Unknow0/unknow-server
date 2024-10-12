package unknow.server.servlet.http2.frame;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.util.io.Buffers;

public class FrameReader {
	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> new FrameReader(p, size, flags, id);

	protected final Http2Processor p;
	protected int size;
	protected int flags;
	protected int id;

	protected FrameReader(Http2Processor p, int size, int flags, int id) {
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
	 * @throws InterruptedException
	 */
	protected int readPad(Buffers buf) throws InterruptedException {
		if ((flags & 0x8) == 0)
			return 0;
		flags &= ~0x8;

		int pad = buf.read(false);
		if (pad >= size) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return -1;
		}
		return pad;
	}

	/**
	 * @param buf
	 * @return this or null
	 * @throws InterruptedException
	 */
	public FrameReader process(Buffers buf) throws InterruptedException {
		size -= buf.skip(size);
		return size == 0 ? null : this;
	}

	public interface FrameBuilder {
		FrameReader build(Http2Processor p, int size, int flags, int id, Buffers buf) throws InterruptedException;
	}
}