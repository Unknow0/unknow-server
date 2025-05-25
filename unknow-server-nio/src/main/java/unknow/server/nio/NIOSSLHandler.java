package unknow.server.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOWorker.WorkerTask;

public class NIOSSLHandler extends NIOHandlerDelegate {
	private static final Logger logger = LoggerFactory.getLogger(NIOSSLHandler.class);

	private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

	private final SSLContext sslContext;

	private NIOConnection co;
	private SSLEngine sslEngine;
	private ByteBuffer rawIn;
	private ByteBuffer app;
	private boolean handshake;

	private int packetBufferSize;
	private int applicationBufferSize;

	public NIOSSLHandler(SSLContext sslContext, NIOConnectionHandler handler) {
		super(handler);
		this.sslContext = sslContext;
	}

	@Override
	public void onInit(NIOConnection co, long now, SSLEngine e) throws IOException {
		InetSocketAddress remote = co.getRemote();
		this.co = co;
		this.sslEngine = sslContext.createSSLEngine(remote.getHostString(), remote.getPort());
		this.packetBufferSize = sslEngine.getSession().getPacketBufferSize();
		this.applicationBufferSize = sslEngine.getSession().getApplicationBufferSize();
		this.rawIn = ByteBuffer.allocate(packetBufferSize);
		this.app = ByteBuffer.allocate(applicationBufferSize);
		this.handshake = true;

		handler.onInit(co, now, sslEngine);
		sslEngine.beginHandshake();
		checkHandshake(sslEngine.getHandshakeStatus(), now);
	}

	@Override
	public void onHandshakeDone(SSLEngine sslEngine, long now) throws IOException {
		handshake = false;
		logger.trace("{} handshake done {}", co);
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
		rawIn.put(b);

		if (handshake) {
			rawIn.flip();
			SSLEngineResult r = sslEngine.unwrap(rawIn, app);
			rawIn.compact();
			if (checkHandshake(r.getHandshakeStatus(), now))
				return;
		}

		rawIn.flip();
		do {
			SSLEngineResult r = sslEngine.unwrap(rawIn, app);
			logger.trace("unwrap {}", r);
			if (r.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
				app.flip();
				handler.onRead(app, now);
				app = ByteBuffer.allocate(applicationBufferSize);
			} else if (r.getStatus() == Status.BUFFER_UNDERFLOW || r.getStatus() == Status.CLOSED)
				break;
		} while (rawIn.hasRemaining());
		rawIn.compact();

		if (app.position() > 0) {
			app.flip();
			handler.onRead(app, now);
			app = ByteBuffer.allocate(applicationBufferSize);
		}
	}

	@Override
	public ByteBuffer beforeWrite(ByteBuffer b, long now) throws IOException {
		ByteBuffer out = ByteBuffer.allocate(packetBufferSize);
		SSLEngineResult r = sslEngine.wrap(b, out);
		logger.trace("wrap {}", r);
		checkHandshake(r.getHandshakeStatus(), now);
		while (b.hasRemaining()) {
			if (out.remaining() < packetBufferSize)
				out = grow(out, packetBufferSize);
			r = sslEngine.wrap(b, out);
			logger.trace("wrap {}", r);
			checkHandshake(r.getHandshakeStatus(), now);
		}
		return out.flip();
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
					r = sslEngine.unwrap(rawIn, app);
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
