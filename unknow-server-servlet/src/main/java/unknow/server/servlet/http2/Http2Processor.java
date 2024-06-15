package unknow.server.servlet.http2;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.HttpProcessor;
import unknow.server.servlet.impl.ServletResponseImpl;
import unknow.server.util.data.IntArrayMap;
import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersInputStream;
import unknow.server.util.io.BuffersUtils;

public class Http2Processor implements HttpProcessor, Http2FlowControl {
	private static final Logger logger = LoggerFactory.getLogger(Http2Processor.class);

	private static final byte[] PRI = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

	private static final int NO_ERROR = 0;
	private static final int PROTOCOL_ERROR = 1;
	private static final int INTERNAL_ERROR = 2;
	private static final int FLOW_CONTROL_ERROR = 3;
	private static final int SETTINGS_TIMEOUT = 4;
	private static final int STREAM_CLOSED = 5;
	private static final int FRAME_SIZE_ERROR = 6;
	private static final int REFUSED_STREAM = 7;
	private static final int CANCEL = 8;
	private static final int COMPRESSION_ERROR = 9;
	private static final int CONNECT_ERROR = 10;
	private static final int ENHANCE_YOUR_CALM = 12;
	private static final int INADEQUATE_SECURITY = 13;
	private static final int HTTP_1_1_REQUIRED = 14;

	private final HttpConnection co;
	private final IntArrayMap<Http2Stream> streams;
	private final Http2Headers headers;
	private final byte[] b;
	private int window;

	private boolean allowPush;
	private int concurrent;
	private int initialWindow;
	private int frame;
	private int headerList;
	private FrameReader r;

	private int lastId;

	private boolean wantContinuation;

	public Http2Processor(HttpConnection co) throws InterruptedException {
		this.co = co;
		this.streams = new IntArrayMap<>();
		this.headers = new Http2Headers(4096);
		this.b = new byte[9];
		this.window = 65535;

		this.allowPush = true;
		this.concurrent = Integer.MAX_VALUE;
		this.initialWindow = 65535;
		this.frame = 16384;
		this.headerList = Integer.MAX_VALUE;
		byte[] b = new byte[9];
		formatFrame(b, 0, 4, 0, 0);
		co.pendingWrite.write(b);
		co.flush();
	}

	@Override
	public void process() throws InterruptedException {
		if (r != null) {
			r = r.process(co.pendingRead);
			if (r != null)
				return;
		}

		while (co.pendingRead.length() > 9 && r == null)
			readFrame(co.pendingRead);
	}

	@Override
	public void add(int v) {
		window += v;
		if (window < 0)
			window = Integer.MAX_VALUE;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public void close() {
		for (Http2Stream s : streams.values())
			s.close(true);
		streams.clear();
	}

	/**
	 * return true if the frame is done
	 * @param buf
	 * @return
	 * @throws InterruptedException
	 */
	private void readFrame(Buffers buf) throws InterruptedException {
		buf.read(b, 0, 9, false);
		int size = (b[0] & 0xff) << 16 | (b[1] & 0xff) << 8 | (b[2] & 0xff);
		int type = b[3];
		int flags = b[4];
		int id = (b[5] & 0x7f) << 24 | (b[6] & 0xff) << 16 | (b[7] & 0xff) << 8 | (b[8] & 0xff);
		logger.debug("{} {} {} {}", size, type, flags, id);

		if (wantContinuation && type != 9 || !wantContinuation && type == 9) {
			goaway(PROTOCOL_ERROR);
			return;
		}
		wantContinuation = false;

		Http2Stream s;
		int pad = 0;
		switch (type) {
			case 0: // data
				s = streams.get(id);
				if (s == null) {
					goaway(PROTOCOL_ERROR);
					return;
				}

				if ((flags & 0x8) == 1) {
					pad = buf.read(false);
					if (pad >= size) {
						goaway(PROTOCOL_ERROR);
						return;
					}
				}

				r = new FrameData(size, flags, id, pad).process(buf);
				return;
			case 1: // header
				if (id == 0 || streams.contains(id)) {
					goaway(PROTOCOL_ERROR);
					return;
				}

				s = new Http2Stream(co, id, this, initialWindow);

				if ((flags & 0x1) == 1) // END_STREAM 
					s.close(false);
				else
					streams.set(id, s);
				if ((flags & 0x8) == 1) {
					pad = buf.read(false);
					if (pad >= size) {
						goaway(PROTOCOL_ERROR);
						return;
					}
				}
				if ((flags & 0x20) == 1) { // PRIORITY
					// TODO priority
					buf.skip(5);
				}

				r = new FrameHeader(size, flags, id, pad, s).process(buf);
				return;
			case 2: // priority
				r = new FrameReader(size, flags, id).process(buf);
				return;
			case 3: // rst_stream
				s = streams.remove(id);
				if (s == null) {
					goaway(PROTOCOL_ERROR);
					return;
				}
				if (size != 4) {
					goaway(FRAME_SIZE_ERROR);
					return;
				}

				buf.read(b, 0, 4, false);
				int err = (b[0] & 0xff) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
				logger.info("close stream {}, err: {}", id, err);
				s.close(true);
				return;
			case 4: // settings
				if ((flags & 0x1) == 1) {
					if (size != 0)
						goaway(FRAME_SIZE_ERROR);
					logger.info("SETTINGS ACK");
					return;
				}
				if (id != 0) {
					goaway(PROTOCOL_ERROR);
					return;
				}
				if (size % 6 != 0) {
					goaway(FRAME_SIZE_ERROR);
					return;
				}
				r = new FrameSettings(size, flags, id).process(buf);
				return;
			case 5: // push promise
				// XXX
				r = new FrameReader(size, flags, id).process(buf);
				return;
			case 6:	// ping
				if (id != 0) {
					goaway(PROTOCOL_ERROR);
					return;
				}
				if (size != 8) {
					goaway(FRAME_SIZE_ERROR);
					return;
				}
				if ((flags & 0x1) == 1)
					break;
				r = new FramePing(size, flags, id).process(buf);
				return;
			case 7: // go away
				if (id != 0) {
					goaway(PROTOCOL_ERROR);
					return;
				}
				r = new FrameGoAway(size, flags, id).process(buf);
				return;
			case 8: // window update
				if (size != 4) {
					goaway(FRAME_SIZE_ERROR);
					return;
				}
				r = new FrameWindowUpdate(size, flags, id).process(buf);
				return;
			case 9: // continuation
				s = streams.get(id);
				if (s == null) {
					goaway(PROTOCOL_ERROR);
					return;
				}

				r = new FrameHeader(size, flags, id, pad, s).process(buf);
				return;
			default:
				goaway(PROTOCOL_ERROR);
		}
	}

	private void goaway(int err) throws InterruptedException {
		byte[] b = new byte[17];
		formatFrame(b, 8, 7, 0, 0);
		b[9] = (byte) ((lastId >> 24) & 0x7f);
		b[10] = (byte) ((lastId >> 16) & 0xff);
		b[11] = (byte) ((lastId >> 8) & 0xff);
		b[12] = (byte) (lastId & 0xff);
		b[13] = (byte) ((err >> 24) & 0x7f);
		b[14] = (byte) ((err >> 16) & 0xff);
		b[15] = (byte) ((err >> 8) & 0xff);
		b[16] = (byte) (err & 0xff);
		synchronized (co) {
			co.pendingWrite.write(b);
			co.flush();
		}
	}

	@SuppressWarnings("resource")
	public void sendHeaders(int id, ServletResponseImpl res) throws InterruptedException {
		byte[] b = new byte[9];
		Buffers out = new Buffers();
		int type = 1;
		synchronized (headers) {
			headers.writeHeader(out, ":status", Integer.toString(res.getStatus()));
			for (String n : res.getHeaderNames()) {
				for (String v : res.getHeaders(n)) {
					headers.writeHeader(out, n, v);

					if (out.length() >= frame) {
						formatFrame(b, out.length(), type, 0, id);
						type = 9;
						synchronized (co) {
							co.pendingWrite.write(b);
							out.read(co.pendingWrite, frame, false);
							co.flush();
						}
					}
				}
			}
		}
		int f = 0x4;

		Http2ServletOutput sout = (Http2ServletOutput) res.getRawStream();
		if (sout == null || sout.isDone())
			f |= 0x1;
		formatFrame(b, out.length(), type, f, id);
		synchronized (co) {
			co.pendingWrite.write(b);
			out.read(co.pendingWrite, -1, false);
			co.flush();
		}
	}

	public void sendData(int id, Buffers data, boolean done) throws InterruptedException {
		byte[] b = new byte[9];
		int l = data.length();
		while (l > frame) {
			formatFrame(b, frame, 0, done ? 0x1 : 0, id);
			synchronized (co) {
				co.pendingWrite.write(b);
				data.read(co.pendingWrite, frame, false);
				co.flush();
			}
		}
		formatFrame(b, data.length(), 0, done ? 0x1 : 0, id);
		synchronized (co) {
			co.pendingWrite.write(b);
			data.read(co.pendingWrite, l, false);
			co.flush();
		}
	}

	public static void formatFrame(byte[] b, int size, int type, int flags, int id) {
		b[0] = (byte) ((size >> 16) & 0xff);
		b[1] = (byte) ((size >> 8) & 0xff);
		b[2] = (byte) (size & 0xff);
		b[3] = (byte) (type & 0xff);
		b[4] = (byte) (flags & 0xff);
		b[5] = (byte) ((id >> 24) & 0x7f);
		b[6] = (byte) ((id >> 16) & 0xff);
		b[7] = (byte) ((id >> 8) & 0xff);
		b[8] = (byte) (id & 0xff);
	}

	private static String error(int err) {
		switch (err) {
			case 0:
				return "NO_ERROR";
			case 1:
				return "PROTOCOL_ERROR";
			case 2:
				return "INTERNAL_ERROR";
			case 3:
				return "FLOW_CONTROL_ERROR";
			case 4:
				return "SETTINGS_TIMEOUT";
			case 5:
				return "STREAM_CLOSED";
			case 6:
				return "FRAME_SIZE_ERROR";
			case 7:
				return "REFUSED_STREAM";
			case 8:
				return "CANCEL";
			case 9:
				return "COMPRESSION_ERROR";
			case 10:
				return "CONNECT_ERROR";
			case 11:
				return "ENHANCE_YOUR_CALM";
			case 12:
				return "INADEQUATE_SECURITY";
			case 13:
				return "HTTP_1_1_REQUIRED";
			default:
				return "unknown " + err;
		}
	}

	public static final HttpProcessorFactory Factory = co -> {
		if (BuffersUtils.startsWith(co.pendingRead, PRI, 0, PRI.length)) {
			co.pendingRead.skip(PRI.length);
			return new Http2Processor(co);
		}
		return null;
	};

	private static class FrameReader {
		protected int size;
		protected int flags;
		protected int id;

		protected FrameReader(int size, int flags, int id) {
			this.size = size;
			this.flags = flags;
			this.id = id;
		}

		/**
		 * @param buf
		 * @return this or null
		 * @throws InterruptedException
		 * @throws IOException 
		 */
		public FrameReader process(Buffers buf) throws InterruptedException {
			size -= buf.skip(size);
			return size == 0 ? null : this;
		}
	}

	private class FrameSettings extends FrameReader {

		private final byte[] b;

		protected FrameSettings(int size, int flags, int id) {
			super(size, flags, id);
			b = new byte[6];
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
						logger.debug("	SETTINGS_HEADER_TABLE_SIZE {}", v);
						synchronized (headers) {
							headers.setMax(v);
						}
						break;
					case 2:
						logger.debug("	SETTINGS_ENABLE_PUSH  {}", v);
						if (v < 0 || v > 1)
							; // send PROTOCOL_ERROR
						allowPush = v == 1;
						break;
					case 3:
						logger.debug("	SETTINGS_MAX_CONCURRENT_STREAMS  {}", v);
						concurrent = v;
						break;
					case 4:
						logger.debug("	SETTINGS_INITIAL_WINDOW_SIZE   {}", v);
						initialWindow = v;
						break;
					case 5:
						logger.debug("	SETTINGS_MAX_FRAME_SIZE  {}", v);
						if (v < 16384 || v > 16777215)
							; // send PROTOCOL_ERROR
						frame = v;
						break;
					case 6:
						logger.debug("	SETTINGS_MAX_HEADER_LIST_SIZE  {}", v);
						headerList = v;
						break;
					default:
						// ignore
				}
			}

			if (size != 0)
				return this;

			byte[] b = new byte[9];
			formatFrame(b, 0, 4, 1, 0);
			synchronized (co) {
				co.pendingWrite.write(b);
				co.flush();
			}
			return null;
		}
	}

	private class FramePing extends FrameReader {

		private final byte[] b;

		protected FramePing(int size, int flags, int id) {
			super(size, flags, id);
			b = new byte[9 + 8];

		}

		@Override
		public final FrameReader process(Buffers buf) throws InterruptedException {
			if (buf.length() < 8)
				return this;

			buf.read(b, 9, 8, false);
			formatFrame(b, 8, 8, 1, 0);
			synchronized (co) {
				co.pendingWrite.write(b);
				co.flush();
			}
			return null;
		}
	}

	private class FrameGoAway extends FrameReader {

		private final byte[] b;

		private int lastId = -1;

		protected FrameGoAway(int size, int flags, int id) {
			super(size, flags, id);
			b = new byte[4];
		}

		@Override
		public FrameReader process(Buffers buf) throws InterruptedException {
			if (lastId >= 0)
				return super.process(buf);
			if (buf.length() < 8)
				return this;

			buf.read(b, 0, 4, false);
			lastId = (b[0] & 0x7f) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);

			buf.read(b, 0, 4, false);
			int err = (b[0] & 0x7f) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
			logger.info("goaway last: {} err: {}", lastId, error(err));
			size -= 8;
			return super.process(buf);
		}
	}

	private class FrameWindowUpdate extends FrameReader {
		private final byte[] b;

		protected FrameWindowUpdate(int size, int flags, int id) {
			super(size, flags, id);
			b = new byte[4];
		}

		@Override
		public final FrameReader process(Buffers buf) throws InterruptedException {
			if (buf.length() < 4)
				return this;
			buf.read(b, 0, 4, false);
			int v = (b[0] & 0x7f) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
			if (v == 0) {
				goaway(PROTOCOL_ERROR);
				return null;
			}

			logger.debug("	window update {}", v);

			Http2FlowControl f = id == 0 ? Http2Processor.this : streams.get(id);
			if (f != null)
				f.add(v);
			return null;
		}
	}

	private class FrameHeader extends FrameReader {
		private final Http2Stream s;
		private final Buffers remain;
		private int pad;

		protected FrameHeader(int size, int flags, int id, int pad, Http2Stream s) {
			super(size, flags, id);
			this.s = s;
			this.remain = new Buffers();
			this.pad = pad;
		}

		@SuppressWarnings("resource")
		@Override
		public final FrameReader process(Buffers buf) throws InterruptedException {
			try {
				buf.prepend(remain);
				BuffersInputStream in = new BuffersInputStream(buf);
				try {
					synchronized (headers) {
						while (in.readCount() < size - pad) {
							in.mark(4096);
							headers.readHeader(in, s::addHeader);
						}
					}
				} catch (@SuppressWarnings("unused") EOFException e) {
					in.writeMark(remain);
					return this;
				} finally {
					logger.debug("	header read: {}", in.readCount());
					size -= in.readCount();
				}

				if (pad > 0) {
					pad -= in.skip(pad);
					if (pad > 0)
						return this;
				}

				wantContinuation = (flags & 0x4) == 0;
				if (!wantContinuation)
					s.start();

				return null;
			} catch (IOException e) {
				logger.error("Failed to parse headers", e);
				error(PROTOCOL_ERROR);
				return null;
			}
		}
	}

	private class FrameData extends FrameReader {
		private final Http2Stream s;
		private int pad;

		protected FrameData(int size, int flags, int id, int pad) {
			super(size - pad, flags, id);
			this.s = streams.get(id);
			this.pad = pad;
		}

		@Override
		public FrameReader process(Buffers buf) throws InterruptedException {
			int l = Math.min(buf.length(), size);
			s.in.read(buf, l);
			size -= l;
			if (size > 0)
				return this;

			if (pad > 0) {
				pad -= buf.skip(pad);
				if (pad > 0)
					return this;
			}

			if ((flags & 0x1) == 1) {
				streams.remove(id);
				s.close(false);
			}
			return null;
		}

	}
}
