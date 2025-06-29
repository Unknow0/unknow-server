package unknow.server.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOWorker.WorkerTask;
import unknow.server.util.io.ByteBuffers;

public class NIOSSLHandler extends NIOHandlerDelegate {
	private static final Logger logger = LoggerFactory.getLogger(NIOSSLHandler.class);

	private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

	private final SSLContext sslContext;

	private NIOConnection co;
	private SSLEngine sslEngine;
	private ByteBuffer rawIn;
	private ByteBuffers rawOut;
	private ByteBuffers app;
	private boolean handshake;

	private int packetBufferSize;
	private int applicationBufferSize;

	public NIOSSLHandler(SSLContext sslContext, NIOConnectionHandler handler) {
		super(handler);
		this.sslContext = sslContext;
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
		this.applicationBufferSize = sslEngine.getSession().getApplicationBufferSize();
		this.rawIn = ByteBuffer.allocate(packetBufferSize);
		this.rawOut = new ByteBuffers(16);
		this.app = new ByteBuffers(16);
		for (int i = 0; i < applicationBufferSize; i += 4096)
			app.accept(ByteBuffer.allocate(4096));

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
			SSLEngineResult r = sslEngine.unwrap(rawIn, app.buf, 0, app.len);
			logger.trace("unwrap {}", r);
			rawIn.compact();
			if (checkHandshake(r.getHandshakeStatus(), now))
				return;
			app.collect(buf -> handler.onRead(buf, now));
		}
		rawIn.flip();
		while (rawIn.hasRemaining()) {
			SSLEngineResult r = sslEngine.unwrap(rawIn, app.buf, 0, app.len);
			logger.trace("unwrap {}", r);
			if (r.getStatus() == Status.BUFFER_UNDERFLOW || r.getStatus() == Status.CLOSED)
				break;
			if (r.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
				for (int i = app.remaining(); i < applicationBufferSize; i += 4096)
					app.accept(ByteBuffer.allocate(4096));
			} else
				app.collect(buf -> handler.onRead(buf, now));
		}
		rawIn.compact();
	}

	@Override
	public void prepareWrite(ByteBuffer b, long now, Consumer<ByteBuffer> c) throws IOException {
		handler.prepareWrite(b, now, rawOut);
	}

	@Override
	public void beforeWrite(long now, Consumer<ByteBuffer> c) throws IOException {
		while (!rawOut.isEmpty()) {
			ByteBuffer out = ByteBuffer.allocate(packetBufferSize);
			SSLEngineResult r = sslEngine.wrap(rawOut.buf, 0, rawOut.len, out);
			logger.trace("wrap {}", r);
			c.accept(out.flip());
			rawOut.compact();
			checkHandshake(r.getHandshakeStatus(), now);
		}
	}

	@Override
	public boolean hasPendingWrites() {
		return !rawOut.isEmpty() || handler.hasPendingWrites();
	}

	@Override
	public void onOutputClosed() {
		sslEngine.closeOutbound();
		try {
			co.write(EMPTY);
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		handler.onOutputClosed();
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
					r = sslEngine.unwrap(rawIn, app.buf, 0, app.len);
					logger.trace("unwrap {}", r);
					rawIn.compact();
					if (r.getStatus() == Status.BUFFER_UNDERFLOW) {
						co.toggleKeyOps();
						return true;
					}
					hs = r.getHandshakeStatus();
					break;
				case NEED_WRAP:
					try {
						co.write(EMPTY);
						return true;
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new SSLException("Handshake interrupted", e);
					}
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
