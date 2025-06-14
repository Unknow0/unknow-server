package unknow.server.servlet.http2.frame;

import java.nio.ByteBuffer;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.http2.Http2Stream;

public class Http2Frame {
	public final byte[] b;
	public int l;

	public Http2Stream s;
	public int size;
	public int type;
	public int flags;
	public int id;
	public int pad;

	public int skip;
	public int lastId;

	public Http2Frame() {
		this.b = new byte[9];
	}

	/**
	 * try to read a frame from buf
	 * @param buf the buf to read
	 * @return true if the frame is full
	 */
	public boolean read(ByteBuffer buf) {
		int m = Math.min(9 - l, buf.remaining());
		buf.get(b, l, m);
		l += m;
		if (l < 9)
			return false;
		l = 0;
		size = (b[0] & 0xff) << 16 | (b[1] & 0xff) << 8 | (b[2] & 0xff);
		type = b[3];
		flags = b[4];
		id = (b[5] & 0x7f) << 24 | (b[6] & 0xff) << 16 | (b[7] & 0xff) << 8 | (b[8] & 0xff);
		return true;
	}

	/**
	* read the size of the padding field if the flags is set (and unset it)
	* 
	* @param p the processor
	* @param buf where to read
	*/
	public boolean readPad(Http2Processor p, ByteBuffer buf) {
		if ((flags & 0x8) == 0)
			return false;
		flags &= ~0x8;

		pad = buf.get() & 0xFF;
		if (pad >= size--)
			p.goaway(Http2Processor.PROTOCOL_ERROR);
		return true;
	}

	@Override
	public String toString() {
		return String.format("%02x, size: %s, flags: %02x, id: %s", type, size, flags, id);
	}
}