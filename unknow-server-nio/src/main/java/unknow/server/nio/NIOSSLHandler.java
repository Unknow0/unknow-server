package unknow.server.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOWorker.WorkerTask;
import unknow.server.util.io.ByteBuffers;

public class NIOSSLHandler extends NIOHandlerDelegate {
	private static final Logger logger = LoggerFactory.getLogger(NIOSSLHandler.class);

	private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

	private final SSLContext sslContext;
	private final ByteBuffers rawOut;

	private NIOConnection co;
	private SSLEngine sslEngine;
	private ByteBuffer rawIn;
	private ByteBuffer app;
	private boolean handshake;

	private int packetBufferSize;

	public NIOSSLHandler(SSLContext sslContext, NIOConnectionHandler handler) {
		super(handler);
		this.sslContext = sslContext;
		this.rawOut = new ByteBuffers(16);
	}

	@Override
	public boolean asyncInit() {
		return true;
	}

	@Override
	public void init(NIOConnection co, long now, SSLEngine e) throws IOException {
		InetSocketAddress remote = co.getRemote();
		this.co = co;
		this.sslEngine = sslContext.createSSLEngine(remote.getHostString(), remote.getPort());
		this.packetBufferSize = sslEngine.getSession().getPacketBufferSize();
		this.rawIn = ByteBuffer.allocate(packetBufferSize);
		this.app = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
		this.handshake = true;

		handler.init(co, now, sslEngine);
		sslEngine.beginHandshake();
		checkHandshake(sslEngine.getHandshakeStatus(), now);
	}

	@Override
	public void onHandshakeDone(SSLEngine sslEngine, long now) throws IOException {
		handshake = false;
		logger.trace("{} handshake done", co);
		handler.onHandshakeDone(sslEngine, now);
		if (rawIn.position() > 0)
			onRead(EMPTY, now);
	}

	public static ByteBuffer grow(ByteBuffer b, int s) {
		ByteBuffer n = ByteBuffer.allocate(b.capacity() + s);
		n.put(b.flip());
		return n;
	}

	@Override
	public void onRead(ByteBuffer b, long now) throws IOException {
		if (b.remaining() > rawIn.remaining())
			rawIn = grow(rawIn, b.remaining());
		if (b.hasRemaining())
			rawIn.put(b);

		if (handshake) {
			rawIn.flip();
			SSLEngineResult r = sslEngine.unwrap(rawIn, app);
			logger.debug("unwrap {}", r);
			rawIn.compact();
			if (checkHandshake(r.getHandshakeStatus(), now))
				return;
			handler.onRead(app.flip(), now);
			app.clear();
		}
		rawIn.flip();
		while (rawIn.hasRemaining()) {
			SSLEngineResult r = sslEngine.unwrap(rawIn, app);
			logger.debug("unwrap {}", r);
			if (r.getStatus() == Status.BUFFER_UNDERFLOW || r.getStatus() == Status.CLOSED)
				break;
			handler.onRead(app.flip(), now);
			app.clear();
		}
		rawIn.compact();
	}

	@Override
	public void transformWrite(ByteBuffer in, ByteBuffers writes, long now) throws IOException {
		rawOut.accept(in);
		while (!rawOut.isEmpty()) {
			ByteBuffer net = ByteBuffer.allocate(packetBufferSize);
			SSLEngineResult r = sslEngine.wrap(rawOut.buf, 0, rawOut.len, net);
			logger.debug("wrap {}", r);
			if (net.position() > 0) {
				handler.transformWrite(net.flip(), writes, now);
				net = null;
			}
			if (r.getStatus() == Status.CLOSED) {
				AtomicInteger l = new AtomicInteger(0);
				rawOut.drain(b -> l.getAndAdd(b.remaining()));
				if (l.get() > 0)
					logger.warn("{} remaining data {}", co, l);
				break;
			}
			while (r.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) {
				if (net == null)
					net = ByteBuffer.allocate(packetBufferSize);
				r = sslEngine.wrap(rawOut.buf, 0, rawOut.len, net);
				if (net.position() > 0) {
					handler.transformWrite(net.flip(), writes, now);
					net = null;
				}
				logger.debug("wrap {}", r);
			}
			checkHandshake(r.getHandshakeStatus(), now);
			rawOut.compact();
		}
	}

	@Override
	public boolean hasPendingWrites() {
		return !rawOut.isEmpty() || handler.hasPendingWrites();
	}

	@Override
	public boolean finishClosing(long now) {
		if (sslEngine.isOutboundDone())
			return true;
		if (!handler.finishClosing(now))
			return false;

		sslEngine.closeOutbound();
		try {
			co.write(EMPTY);
		} catch (@SuppressWarnings("unused") IOException e) { // ok
		}
		return false;
	}

	private boolean checkHandshake(HandshakeStatus hs, long now) throws IOException {
		while (true) {
			SSLEngineResult r;
			switch (hs) {
				case NEED_TASK:
					logger.trace("running tasks");
					co.submit(new RunTask());
					return true;
				case NEED_UNWRAP:
				case NEED_UNWRAP_AGAIN:
					rawIn.flip();
					r = sslEngine.unwrap(rawIn, app);
					logger.trace("unwrap {}", r);
					rawIn.compact();
					if (r.getStatus() == Status.BUFFER_UNDERFLOW)
						return true;
					hs = r.getHandshakeStatus();
					break;
				case NEED_WRAP:
					co.write(EMPTY);
					return true;
				case FINISHED:
					onHandshakeDone(sslEngine, now);
					// fallthrough
				default:
					return false;
			}
		}
	}

	private final class RunTask implements Runnable, WorkerTask {
		@Override
		public void run() {
			logger.trace("start task");
			Runnable task;
			while ((task = sslEngine.getDelegatedTask()) != null)
				task.run();
			co.execute(this);
		}

		@Override
		public void run(long now) {
			logger.trace("resume handshake");
			try {
				checkHandshake(sslEngine.getHandshakeStatus(), now);
			} catch (Exception e) {
				logger.warn("Failed to handshake", e);
			}
		}
	}
}
