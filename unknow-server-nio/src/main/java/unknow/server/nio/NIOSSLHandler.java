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

	public NIOSSLHandler(SSLContext sslContext, NIOConnectionHandler handler) {
		super(handler);
		this.sslContext = sslContext;
	}

	@Override
	public void onInit(NIOConnection co, long now, SSLEngine e) throws IOException {
		InetSocketAddress remote = co.getRemote();
		this.co = co;
		this.sslEngine = sslContext.createSSLEngine(remote.getHostString(), remote.getPort());
		this.rawIn = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
		this.app = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
		this.handshake = true;

		handler.onInit(co, now, sslEngine);
		sslEngine.beginHandshake();
		checkHandshake(sslEngine.getHandshakeStatus());
	}

	@Override
	public void onHandshakeDone(SSLEngine sslEngine) throws IOException {
		handshake = false;
		handler.onHandshakeDone(sslEngine);
	}

	@Override
	public void onRead(ByteBuffer b, long now) throws IOException {
		if (b.remaining() > rawIn.remaining()) {
			ByteBuffer n = ByteBuffer.allocate(rawIn.capacity() + b.remaining());
			n.put(rawIn.flip());
			rawIn = n;
		}
		rawIn.put(b);

		if (handshake) {
			rawIn.flip();
			SSLEngineResult r = sslEngine.unwrap(rawIn, app);
			rawIn.compact();
			if (checkHandshake(r.getHandshakeStatus())) {
				return;
			}
		}

		rawIn.flip();
		do {
			SSLEngineResult r = sslEngine.unwrap(rawIn, app);
			logger.trace("unwrap {}", r);
			if (r.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
				app.flip();
				handler.onRead(app, now);
				app = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
			} else if (r.getStatus() == Status.BUFFER_OVERFLOW || r.getStatus() == Status.CLOSED)
				break;
		} while (rawIn.hasRemaining());
		rawIn.compact();

		if (app.position() > 0) {
			app.flip();
			handler.onRead(app, now);
			app = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
		}
	}

	@Override
	public ByteBuffer beforeWrite(ByteBuffer b) throws IOException {
		ByteBuffer out = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
		SSLEngineResult r = sslEngine.wrap(b, out);
		logger.trace("wrap {}", r);
		checkHandshake(r.getHandshakeStatus());
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

	private boolean checkHandshake(HandshakeStatus hs) throws IOException {
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
					onHandshakeDone(sslEngine);
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
				checkHandshake(sslEngine.getHandshakeStatus());
			} catch (Exception e) {
				logger.warn("Failed to handshake", e);
			}
		}
	}
}
