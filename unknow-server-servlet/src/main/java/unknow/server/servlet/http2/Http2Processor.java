package unknow.server.servlet.http2;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.HttpProcessor;
import unknow.server.util.data.IntArrayMap;
import unknow.server.util.io.Buffers;
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
	private int window;

	private boolean allowPush;
	private int concurrent;
	private int initialWindow;
	private int frame;
	private int headerList;
	private FrameReader r;

	private int lastId;

	public Http2Processor(HttpConnection co) throws InterruptedException {
		this.co = co;
		this.streams = new IntArrayMap<>();
		this.headers = new Http2Headers(4096);
		this.window = 65535;

		this.allowPush = true;
		this.concurrent = Integer.MAX_VALUE;
		this.initialWindow = 65535;
		this.frame = 16384;
		this.headerList = Integer.MAX_VALUE;
		byte[] b = new byte[9];
		format(b, 0, 4, 0, 0);
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
	}

	private static final byte[] b = new byte[9];

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

		switch (type) {
			case 0: // data
				break;
			case 1: // header
				if (id == 0) {
					goaway(PROTOCOL_ERROR);
					return;
				}

				Http2Stream s = new Http2Stream(co, initialWindow);
				streams.set(id, s);

				try {
					Buffers b = new Buffers();
					b.read(co.pendingRead, size, false);
					headers.readHeaders(co.pendingRead, (k, v) -> logger.debug("	{}: {}", k, v));
				} catch (Exception e) {
					logger.error("Failed to parse headers", e);
					error(PROTOCOL_ERROR);
				}

				// TODO
				break;
			case 2: // priority
				break;
			case 3: // rst_stream
				co.pendingRead.read(b, 0, 4, false);
				int err = (b[0] & 0xff) << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
				logger.debug("close stream {}, err: {}", id, err);
				// TODO close stream id
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
				}
				if (size % 6 != 0) {
					goaway(FRAME_SIZE_ERROR);
					return;
				}
				r = new FrameSettings(size, flags, id).process(buf);
				return;
			case 5: // push promise
				break;
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
				// TODO
				return;
			case 8: // window update
				if (size != 4) {
					goaway(FRAME_SIZE_ERROR);
					return;
				}
				r = new FrameWindowUpdate(size, flags, id).process(buf);
				return;
			case 9: // continuation
				// id == 0 send PROTOCOL_ERROR
				// if previous frame != HEADERS, PUSH_PROMISE or CONTINUATION send PROTOCOL_ERROR
			default:
		}

		co.pendingRead.skip(size);
	}

	private void goaway(int err) throws InterruptedException {
		byte[] b = new byte[17];
		format(b, 8, 7, 0, 0);
		b[9] = (byte) ((lastId >> 24) & 0x7f);
		b[10] = (byte) ((lastId >> 16) & 0xff);
		b[11] = (byte) ((lastId >> 8) & 0xff);
		b[12] = (byte) (lastId & 0xff);
		b[13] = (byte) ((err >> 24) & 0x7f);
		b[14] = (byte) ((err >> 16) & 0xff);
		b[15] = (byte) ((err >> 8) & 0xff);
		b[16] = (byte) (err & 0xff);
		co.pendingWrite.write(b);
		co.flush();
	}

	private void format(byte[] b, int size, int type, int flags, int id) {
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

	private static abstract class FrameReader {
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
		 */
		public abstract FrameReader process(Buffers buf) throws InterruptedException;
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
						headers.setMax(v);
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
			format(b, 0, 4, 1, 0);
			co.pendingWrite.write(b);
			co.flush();
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
			format(b, 8, 8, 1, 0);
			co.pendingWrite.write(b);
			co.flush();
			return null;
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
		private final byte[] b;
		private int pad = 0;

		protected FrameHeader(int size, int flags, int id) {
			super(size, flags, id);
			b = new byte[4];
		}

		@Override
		public final FrameReader process(Buffers buf) throws InterruptedException {
			if ((flags & 0x1) != 0) // END_STREAM 
				; // END_STREAM
			if ((flags & 0x4) != 0) // END_HEADERS
				;
			if ((flags & 0x8) != 0) { // PADDED
				if (co.pendingRead.length() < 1)
					return this;
				pad = (co.pendingRead.read(false) & 0xff);
				flags ^= 0x8;
			}

			if ((flags & 0x20) != 0) { // PRIORITY
				if (co.pendingRead.length() < 5)
					return this;
				// TODO priority
				co.pendingRead.skip(5);
				flags ^= 0x20;
			}

			return null;
		}
	}
}
