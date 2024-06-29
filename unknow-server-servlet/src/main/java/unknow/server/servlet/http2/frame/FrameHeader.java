package unknow.server.servlet.http2.frame;

import java.io.EOFException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.http2.Http2Stream;
import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersInputStream;

public class FrameHeader extends FrameReader {
	private static final Logger logger = LoggerFactory.getLogger(FrameHeader.class);

	public static final FrameBuilder BUILDER = (p, size, flags, id, buf) -> {
		if (id == 0 || p.streams.contains(id)) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}

		Http2Stream s = new Http2Stream(p, id, p.initialWindow);
		p.streams.set(id, s);

		return new FrameHeader(p, size, flags, id, s).process(buf);
	};

	public static final FrameBuilder CONTINUATION = (p, size, flags, id, buf) -> {
		Http2Stream s = p.streams.get(id);
		if (s == null) {
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}

		return new FrameHeader(p, size, flags, id, s).process(buf);
	};

	private final Http2Stream s;
	private final Buffers remain;
	private int pad;

	protected FrameHeader(Http2Processor p, int size, int flags, int id, Http2Stream s) {
		super(p, size, flags, id);
		this.s = s;
		this.remain = new Buffers();
		this.pad = -1;
	}

	@SuppressWarnings("resource")
	@Override
	public final FrameReader process(Buffers buf) throws InterruptedException {
		if (pad < 0) {
			pad = readPad(buf);
			if (pad < 0)
				return null;
		}

		if ((flags & 0x20) == 1) { // PRIORITY
			buf.skip(5);
		}

		try {
			buf.prepend(remain);
			BuffersInputStream in = new BuffersInputStream(buf);
			readHeaders(in);
			if (size > pad)
				return this;

			if (pad > 0) {
				pad -= in.skip(pad);
				if (pad > 0)
					return this;
			}

			p.wantContinuation = (flags & 0x4) == 0;
			if (!p.wantContinuation)
				s.start();

			if ((flags & 0x1) == 1) {
				p.streams.remove(id);
				p.pending.add(s);
				s.close(false);
			}

			return null;
		} catch (IOException e) {
			logger.error("Failed to parse headers", e);
			p.goaway(Http2Processor.PROTOCOL_ERROR);
			return null;
		}
	}

	private void readHeaders(BuffersInputStream in) throws InterruptedException, IOException {
		try {
			synchronized (p.headers) {
				while (in.readCount() < size - pad) {
					in.mark(4096);
					p.headers.readHeader(in, s::addHeader);
				}
			}
		} catch (@SuppressWarnings("unused") EOFException e) {
			in.writeMark(remain);
			size += remain.length();
		} finally {
			size -= in.readCount();
		}
	}
}