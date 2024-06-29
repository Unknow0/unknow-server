package unknow.server.servlet.http2;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.HttpProcessor;
import unknow.server.servlet.http2.frame.FrameData;
import unknow.server.servlet.http2.frame.FrameGoAway;
import unknow.server.servlet.http2.frame.FrameHeader;
import unknow.server.servlet.http2.frame.FramePing;
import unknow.server.servlet.http2.frame.FrameReader;
import unknow.server.servlet.http2.frame.FrameReader.FrameBuilder;
import unknow.server.servlet.http2.frame.FrameRstStream;
import unknow.server.servlet.http2.frame.FrameSettings;
import unknow.server.servlet.http2.frame.FrameWindowUpdate;
import unknow.server.servlet.impl.ServletResponseImpl;
import unknow.server.util.data.IntArrayMap;
import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersUtils;

public class Http2Processor implements HttpProcessor, Http2FlowControl {
	static final Logger logger = LoggerFactory.getLogger(Http2Processor.class);

	private static final byte[] PRI = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

	public static final int NO_ERROR = 0;
	public static final int PROTOCOL_ERROR = 1;
	public static final int INTERNAL_ERROR = 2;
	public static final int FLOW_CONTROL_ERROR = 3;
	public static final int SETTINGS_TIMEOUT = 4;
	public static final int STREAM_CLOSED = 5;
	public static final int FRAME_SIZE_ERROR = 6;
	public static final int REFUSED_STREAM = 7;
	public static final int CANCEL = 8;
	public static final int COMPRESSION_ERROR = 9;
	public static final int CONNECT_ERROR = 10;
	public static final int ENHANCE_YOUR_CALM = 12;
	public static final int INADEQUATE_SECURITY = 13;
	public static final int HTTP_1_1_REQUIRED = 14;

	private static final IntArrayMap<FrameBuilder> BUILDERS = new IntArrayMap<>();

	static {
		BUILDERS.set(0, FrameData.BUILDER);
		BUILDERS.set(1, FrameHeader.BUILDER);
		BUILDERS.set(2, FrameReader.BUILDER);
		BUILDERS.set(3, FrameRstStream.BUILDER);
		BUILDERS.set(4, FrameSettings.BUILDER);
		BUILDERS.set(6, FramePing.BUILDER);
		BUILDERS.set(7, FrameGoAway.BUILDER);
		BUILDERS.set(8, FrameWindowUpdate.BUILDER);
		BUILDERS.set(9, FrameHeader.CONTINUATION);
	}

	public final HttpConnection co;
	public final IntArrayMap<Http2Stream> streams;
	public final List<Http2Stream> pending;
	public final Http2Headers headers;
	private final byte[] b;
	private int window;
	public boolean closing;

//	private boolean allowPush;
//	private int concurrent;
	public int initialWindow;
	public int frame;
//	private int headerList;
	private FrameReader r;

	private int lastId;

	public boolean wantContinuation;

	public Http2Processor(HttpConnection co) throws InterruptedException {
		this.co = co;
		this.streams = new IntArrayMap<>();
		this.pending = new LinkedList<>();
		this.headers = new Http2Headers(4096);
		this.b = new byte[9];
		this.closing = false;

		this.window = 65535;

//		this.allowPush = true;
//		this.concurrent = Integer.MAX_VALUE;
		this.initialWindow = 65535;
		this.frame = 16384;
//		this.headerList = Integer.MAX_VALUE;

		formatFrame(b, 0, 4, 0, 0);
		co.pendingWrite.write(b, 0, 9);
		co.flush();
	}

	@Override
	public final void process() throws InterruptedException {
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
	public boolean isClosable(boolean stop) {
		if (stop && !closing) {
			try {
				goaway(NO_ERROR);
				closing = true;
			} catch (@SuppressWarnings("unused") InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		Iterator<Http2Stream> it = pending.iterator();
		while (it.hasNext()) {
			if (it.next().isClosed())
				it.remove();
		}
		return pending.isEmpty() && streams.isEmpty();
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

		if (wantContinuation && type != 9 || !wantContinuation && type == 9) {
			goaway(PROTOCOL_ERROR);
			return;
		}
		wantContinuation = false;

		FrameBuilder b = BUILDERS.get(type);
		if (b == null) {
			goaway(PROTOCOL_ERROR);
			return;
		}

		r = b.build(this, size, flags, id, buf);
	}

	public void goaway(int err) throws InterruptedException {
		byte[] f = new byte[17];
		formatFrame(f, 8, 7, 0, 0);
		f[9] = (byte) ((lastId >> 24) & 0x7f);
		f[10] = (byte) ((lastId >> 16) & 0xff);
		f[11] = (byte) ((lastId >> 8) & 0xff);
		f[12] = (byte) (lastId & 0xff);
		f[13] = (byte) ((err >> 24) & 0x7f);
		f[14] = (byte) ((err >> 16) & 0xff);
		f[15] = (byte) ((err >> 8) & 0xff);
		f[16] = (byte) (err & 0xff);
		Buffers b = co.pendingWrite;
		b.lock();
		try {
			b.write(f);
		} finally {
			b.unlock();
		}
		co.flush();
		logger.debug("{}: send GOAWAY {}", this, error(err));
	}

	public void sendFrame(byte[] b, int size, int type, int flags, int id, Buffers data) throws InterruptedException {
		size = Math.min(size, data.length());
		formatFrame(b, size, type, flags, id);
		Buffers write = co.pendingWrite;
		write.lock();
		try {
			write.write(b);
			data.read(write, size, false);
		} finally {
			write.unlock();
		}
		co.toggleKeyOps();
	}

	public void rawWrite(byte[] b) throws InterruptedException {
		Buffers write = co.pendingWrite;
		write.lock();
		try {
			write.write(b);
		} finally {
			write.unlock();
		}
		co.toggleKeyOps();
	}

	public void sendHeaders(int id, ServletResponseImpl res) throws InterruptedException {
		byte[] f = new byte[9];
		Buffers out = new Buffers();
		int type = 1;
		Buffers write = co.pendingWrite;
		write.lock();
		try {  // all headers frame should be together
			synchronized (headers) {
				headers.writeHeader(out, ":status", Integer.toString(res.getStatus()));
				for (String n : res.getHeaderNames()) {
					for (String v : res.getHeaders(n)) {
						headers.writeHeader(out, n, v);

						while (out.length() >= frame) {
							sendFrame(f, frame, type, 0, id, out);
							type = 9;
						}
					}
				}
			}
			int flag = 0x4;
			Http2ServletOutput sout = (Http2ServletOutput) res.getRawStream();
			if (sout == null || sout.isDone())
				flag |= 0x1;

			sendFrame(f, out.length(), type, flag, id, out);
		} finally {
			write.unlock();
		}
	}

	public void sendData(int id, Buffers data, boolean done) throws InterruptedException {
		byte[] f = new byte[9];
		int l = data.length();
		while (l > frame)
			sendFrame(f, frame, 0, 0, id, data);
		sendFrame(f, data.length(), 0, done ? 0x1 : 0, id, data);
		co.flush();
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

	public static String error(int err) {
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
}
