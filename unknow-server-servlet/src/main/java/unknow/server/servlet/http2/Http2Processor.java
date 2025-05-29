package unknow.server.servlet.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
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
import unknow.server.servlet.http2.frame.FrameRstStream;
import unknow.server.servlet.http2.frame.FrameSettings;
import unknow.server.servlet.http2.frame.FrameWindowUpdate;
import unknow.server.servlet.http2.frame.Http2Frame;
import unknow.server.servlet.http2.header.Http2HeadersDecoder;
import unknow.server.servlet.http2.header.Http2HeadersEncoder;
import unknow.server.servlet.impl.ServletResponseImpl;
import unknow.server.util.ConsumerWithException;
import unknow.server.util.data.ConcurentIntArrayMap;
import unknow.server.util.data.IntArrayMap;

/**
 * processor for http/2 protocol
 */
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

	private static final IntArrayMap<FrameReader> READERS = new IntArrayMap<>();

	static {
		READERS.set(0, FrameData.INSTANCE);
		READERS.set(1, FrameHeader.INSTANCE);
		READERS.set(2, FrameReader.INSTANCE);
		READERS.set(3, FrameRstStream.INSTANCE);
		READERS.set(4, FrameSettings.INSTANCE);
		READERS.set(6, FramePing.INSTANCE);
		READERS.set(7, FrameGoAway.INSTANCE);
		READERS.set(8, FrameWindowUpdate.INSTANCE);
		READERS.set(9, FrameHeader.INSTANCE);
	}

	/** the connection */
	public final HttpConnection co;
	/** stream started */
	public final IntArrayMap<Http2Stream> streams;
	/** stream waiting completion */
	public final IntArrayMap<Http2Stream> pending;
	/** header encoder */
	public final Http2HeadersEncoder headersEncoder;
	/** headers decoder */
	public final Http2HeadersDecoder headersDecoder;

	private final Http2Frame frame;

	private int window;
	private int closingId;

//	private boolean allowPush;
//	private int concurrent;
	/** initial flow control window for stream */
	public int initialWindow;
	/** max frame size */
	public int frameSize;
//	private int headerList;

	private int lastId;

	/** want continuation frame */
	public boolean wantContinuation;

	public Http2Processor(HttpConnection co) throws IOException {
		this.co = co;
		this.streams = new IntArrayMap<>();
		this.pending = new ConcurentIntArrayMap<>();
		this.headersEncoder = new Http2HeadersEncoder(4096);
		this.headersDecoder = new Http2HeadersDecoder(4096);
		this.frame = new Http2Frame();
		this.closingId = -1;

		this.window = 65535;

//		this.allowPush = true;
//		this.concurrent = Integer.MAX_VALUE;
		this.initialWindow = 65535;
		this.frameSize = 16384;
//		this.headerList = Integer.MAX_VALUE;

		// read PRI
		frame.type = -1;
		frame.size = 24;

		sendFrame(4, 0, 0, null);
	}

	@Override
	public void onRead(ByteBuffer b, long now) throws IOException {
		while (b.hasRemaining()) {
			if (frame.size == 0)
				readFrame(b);
			READERS.getOrDefault(frame.type, FrameReader.INSTANCE).process(this, frame, b);
		}
	}

	@Override
	public void add(int v) {
		window += v;
		if (window < 0)
			window = Integer.MAX_VALUE;
	}

	@Override
	public boolean canClose(long now, boolean stop) {
		if (stop)
			return true;
		Iterator<Http2Stream> it = pending.values().iterator();
		while (it.hasNext()) {
			if (it.next().isClosed())
				it.remove();
		}
		if (!pending.isEmpty() || !streams.isEmpty())
			return false;
		return co.keepAliveReached(now);
	}

	@Override
	public void startClose() {
		goaway(NO_ERROR);
		for (Http2Stream s : streams.values())
			s.close(false);
	}

	@Override
	public boolean finishClosing(long now) {
		Iterator<Http2Stream> it = pending.values().iterator();
		while (it.hasNext()) {
			if (it.next().isClosed())
				it.remove();
		}
		return pending.isEmpty() && streams.isEmpty();
	}

	@Override
	public void doneClosing() {
		for (Http2Stream s : streams.values())
			s.close(true);
		for (Http2Stream s : pending.values())
			s.close(true);
	}

	public void close() {
		closingId = lastId;
	}

	public void addStream(Http2Stream s) {
		streams.set(s.id(), s);
		lastId = Math.max(s.id(), lastId);
	}

	public void closeStream(int id) {
		if (id > lastId)
			return;
		Http2Stream s = streams.remove(id);
		if (s == null)
			s = pending.remove(id);
		if (s != null)
			s.close(true);
	}

	/**
	 * return true if the frame is done
	 * 
	 * @param buf
	 * @return
	 * @
	 */
	private void readFrame(ByteBuffer buf) {
		if (!frame.read(buf))
			return;
		logger.debug("{} read frame: {}", co, frame);
		frame.readPad(this, buf);

		if (wantContinuation && frame.type != 9 || !wantContinuation && frame.type == 9)
			goaway(PROTOCOL_ERROR);

		if (closingId > 0 && frame.id > closingId)
			frame.type = -1;

		READERS.getOrDefault(frame.type, FrameReader.INSTANCE).check(this, frame);
	}

	public void goaway(int err) {
		closingId = lastId;
		byte[] f = new byte[17];
		formatFrame(f, 0, 8, 7, 0, 0);
		f[9] = (byte) ((lastId >> 24) & 0x7f);
		f[10] = (byte) ((lastId >> 16) & 0xff);
		f[11] = (byte) ((lastId >> 8) & 0xff);
		f[12] = (byte) (lastId & 0xff);
		f[13] = (byte) ((err >> 24) & 0x7f);
		f[14] = (byte) ((err >> 16) & 0xff);
		f[15] = (byte) ((err >> 8) & 0xff);
		f[16] = (byte) (err & 0xff);
		try {
			synchronized (co) {
				co.write(ByteBuffer.wrap(f));
			}
			logger.info("{}: send GOAWAY {}", co, error(err));
		} catch (IOException e) {
			logger.error("Failed to send", e);
		}
	}

	public void sendFrame(int type, int flags, int id, ByteBuffer data) throws IOException {
		int size = data == null ? 0 : data.remaining();
		if (data == null || data.position() < 9 || size > frameSize) {
			size = Math.min(frameSize, size);
			ByteBuffer b = ByteBuffer.allocate(size + 9);
			if (data != null) {
				System.arraycopy(b.array(), 9, data.array(), data.position(), size);
				data.position(data.position() + size);
			}
			data = b;
		} else
			data.position(data.position() - 9);

		byte[] b = data.array();
		formatFrame(b, data.position(), size, type, flags, id);
		synchronized (co) {
			co.write(data);
		}

		if (logger.isDebugEnabled())
			logger.debug(String.format("%s send frame: %02x, size: %s, flags: %02x, id: %s", co, type, size, flags, id));
		co.flush();
	}

	@SuppressWarnings("resource")
	public void sendHeaders(int id, ServletResponseImpl res) throws IOException {
		List<ByteBuffer> list = new ArrayList<>();
		ConsumerWithException<ByteBuffer, RuntimeException> c = list::add;

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
		if (sout == null || sout.isDone()) {
			flags |= 0x1;
			pending.remove(id);
		}
		int size = list.size();
		if (size == 1) {
			sendFrame(1, flags | 0x4, id, list.get(0));
			return;
		}
		synchronized (co) {
			sendFrame(1, flags, id, list.get(0));
			for (int i = 1; i < size - 1; i++)
				sendFrame(9, flags, id, list.get(i));
			sendFrame(9, flags | 0x4, id, list.get(-1));
		}
	}

	public void sendData(int id, ByteBuffer data, boolean done) throws IOException {
		if (done)
			pending.remove(id);
		if (data != null) {
			while (data.remaining() > frameSize)
				sendFrame(0, 0, id, data);
		}
		sendFrame(0, done ? 0x1 : 0, id, data);
		co.flush();
	}

	public static void formatFrame(byte[] b, int o, int size, int type, int flags, int id) {
		b[o++] = (byte) ((size >> 16) & 0xff);
		b[o++] = (byte) ((size >> 8) & 0xff);
		b[o++] = (byte) (size & 0xff);
		b[o++] = (byte) (type & 0xff);
		b[o++] = (byte) (flags & 0xff);
		b[o++] = (byte) ((id >> 24) & 0x7f);
		b[o++] = (byte) ((id >> 16) & 0xff);
		b[o++] = (byte) ((id >> 8) & 0xff);
		b[o++] = (byte) (id & 0xff);
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
