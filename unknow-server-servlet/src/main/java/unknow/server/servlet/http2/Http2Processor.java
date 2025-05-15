package unknow.server.servlet.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOConnectionHandler;
import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.http2.frame.FrameData;
import unknow.server.servlet.http2.frame.FrameGoAway;
import unknow.server.servlet.http2.frame.FrameHeader;
import unknow.server.servlet.http2.frame.FramePing;
import unknow.server.servlet.http2.frame.FrameReader;
import unknow.server.servlet.http2.frame.FrameReader.FrameBuilder;
import unknow.server.servlet.http2.frame.FrameRstStream;
import unknow.server.servlet.http2.frame.FrameSettings;
import unknow.server.servlet.http2.frame.FrameWindowUpdate;
import unknow.server.servlet.http2.header.Http2HeadersDecoder;
import unknow.server.servlet.http2.header.Http2HeadersEncoder;
import unknow.server.servlet.http2.header.Http2HeadersEncoder.ByteBufferConsumer;
import unknow.server.servlet.impl.ServletResponseImpl;
import unknow.server.util.data.IntArrayMap;

public class Http2Processor implements NIOConnectionHandler, Http2FlowControl {
	static final Logger logger = LoggerFactory.getLogger(Http2Processor.class);

	public static final byte[] PRI = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

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
	public final Http2HeadersEncoder headersEncoder;
	public final Http2HeadersDecoder headersDecoder;
	private final byte[] b;
	private int l;
	private int window;
	private int closingId;

//	private boolean allowPush;
//	private int concurrent;
	public int initialWindow;
	public int frame;
//	private int headerList;
	private FrameReader r;

	private int lastId;

	public boolean wantContinuation;

	public Http2Processor(HttpConnection co) throws IOException {
		this.co = co;
		this.streams = new IntArrayMap<>();
		this.pending = new LinkedList<>();
		this.headersEncoder = new Http2HeadersEncoder(4096);
		this.headersDecoder = new Http2HeadersDecoder(4096);
		this.b = new byte[9];
		this.closingId = -1;

		this.window = 65535;

//		this.allowPush = true;
//		this.concurrent = Integer.MAX_VALUE;
		this.initialWindow = 65535;
		this.frame = 16384;
//		this.headerList = Integer.MAX_VALUE;

		this.r = new FrameReader(this, 24, 0, 0);

		sendFrame(4, 0, 0, null);
	}

	@Override
	public void onRead(ByteBuffer b, long now) throws IOException {
		if (b == null) {
			closingId = lastId;
			return;
		}

		while (b.hasRemaining()) {
			if (r != null)
				r = r.process(b);
			else
				readFrame(b);
		}
	}

	@Override
	public void add(int v) {
		window += v;
		if (window < 0)
			window = Integer.MAX_VALUE;
	}

	@Override
	public boolean closed(long now, boolean stop) {
		if (stop && closingId < 0) {
			try {
				goaway(NO_ERROR);
				closingId = lastId;
			} catch (@SuppressWarnings("unused") IOException e) { // ok
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
	public void onFree() {
		for (Http2Stream s : streams.values())
			s.close(true);
		streams.clear();
	}

	public void close(int lastClienId) {
		closingId = lastClienId;
	}

	public void addStream(Http2Stream s) {
		streams.set(s.id(), s);
		lastId = Math.max(s.id(), lastId);
	}

	/**
	 * return true if the frame is done
	 * 
	 * @param buf
	 * @return
	 * @
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	private void readFrame(ByteBuffer buf) throws IOException {
		int m = Math.min(9 - l, buf.remaining());
		buf.get(b, l, m);
		l += m;
		if (l < 9)
			return;
		l = 0;
		int size = (b[0] & 0xff) << 16 | (b[1] & 0xff) << 8 | (b[2] & 0xff);
		int type = b[3];
		int flags = b[4];
		int id = (b[5] & 0x7f) << 24 | (b[6] & 0xff) << 16 | (b[7] & 0xff) << 8 | (b[8] & 0xff);

		if (wantContinuation && type != 9 || !wantContinuation && type == 9) {
			goaway(PROTOCOL_ERROR);
			return;
		}
		wantContinuation = false;

		if (logger.isDebugEnabled())
			logger.debug(String.format("%s read frame: %02x, size: %s, flags: %02x, id: %s", co, type, size, flags, id));

		FrameBuilder builder;
		if (closingId < 0 || id < closingId) {
			builder = BUILDERS.get(type);
			if (builder == null) {
				goaway(PROTOCOL_ERROR);
				return;
			}
		} else
			builder = FrameReader.BUILDER;

		r = builder.build(this, size, flags, id, buf);
	}

	public void goaway(int err) throws IOException {
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
		co.write(ByteBuffer.wrap(f));
		logger.debug("{}: send GOAWAY {}", co, error(err));
	}

	public void sendFrame(int type, int flags, int id, ByteBuffer data) throws IOException {
		int size = Math.min(frame, data == null ? 0 : data.remaining());
		byte[] b = new byte[9];
		formatFrame(b, size, type, flags, id);
		synchronized (co) {
			co.write(ByteBuffer.wrap(b));
			if (size > 0 && data != null) {
				if (size < data.remaining()) {
					co.write(data.slice().limit(size));
					data.position(data.position() + size);
				} else
					co.write(data);
			}
		}

		if (logger.isDebugEnabled())
			logger.debug(String.format("%s send frame: %02x, size: %s, flags: %02x, id: %s", co, type, size, flags, id));
		co.flush();
	}

	@SuppressWarnings("resource")
	public void sendHeaders(int id, ServletResponseImpl res) throws IOException {
		List<ByteBuffer> list = new ArrayList<>();
		ByteBufferConsumer c = list::add;

		synchronized (headersEncoder) {
			headersEncoder.encode(":status", Integer.toString(res.getStatus()), c);
			for (String n : res.getHeaderNames()) {
				for (String v : res.getHeaders(n))
					headersEncoder.encode(n, v, c);
			}

			headersEncoder.flush(c);
		}

		int flags = 0x0;
		Http2ServletOutput sout = (Http2ServletOutput) res.getRawStream();
		if (sout == null || sout.isDone())
			flags |= 0x1;
		int size = list.size();
		if (size == 1) {
			sendFrame(1, flags | 0x4, id, list.get(0));
			return;
		}
		sendFrame(1, flags, id, list.get(0));
		for (int i = 1; i < size - 1; i++)
			sendFrame(9, flags, id, list.get(i));
		sendFrame(9, flags | 0x4, id, list.get(-1));
	}

	public void sendData(int id, ByteBuffer data, boolean done) throws IOException {
		while (data.remaining() > frame)
			sendFrame(0, 0, id, data);
		sendFrame(0, done ? 0x1 : 0, id, data);
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

}
