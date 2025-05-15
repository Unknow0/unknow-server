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

	public NIOSSLHandler(SSLContext sslContext, NIOConnectionHandler handler) {
		super(handler);
		this.sslContext = sslContext;
	}

	@Override
	public boolean closed(long now, boolean stop) {
		return handler.closed(now, stop);
	}

	@Override
	public void onInit(NIOConnection co, long now, SSLEngine e) throws IOException {
		InetSocketAddress remote = co.getRemote();
		this.co = co;
		this.sslEngine = sslContext.createSSLEngine(remote.getHostString(), remote.getPort());
		this.rawIn = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
		this.app = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());

		handler.onInit(co, now, sslEngine);
		sslEngine.beginHandshake();
		checkHandshake(sslEngine.getHandshakeStatus());
	}

	@Override
	public void onRead(ByteBuffer b, long now) throws IOException {
		if (b == null) {
			handler.onRead(null, now);
			return;
		}
		if (b.remaining() > rawIn.remaining()) {
			ByteBuffer n = ByteBuffer.allocate(rawIn.capacity() + b.remaining());
			n.put(rawIn.flip());
			rawIn = b;
		}
		rawIn.put(b);

		rawIn.flip();
		SSLEngineResult r = sslEngine.unwrap(rawIn, app);
		logger.debug("unwrap {}", r);
		rawIn.compact();

		if (checkHandshake(r.getHandshakeStatus()))
			return;

		if (app.position() > 0) {
			app.flip();
			handler.onRead(app, now);
			app = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());
		}
	}

	private boolean checkHandshake(HandshakeStatus hs) throws IOException {
		while (true) {
			SSLEngineResult r;
			switch (hs) {
				case NEED_TASK:
					logger.debug("running tasks");
					co.submit(new RunTask());
					return true;
				case NEED_UNWRAP:
				case NEED_UNWRAP_AGAIN:
					rawIn.flip();
					r = sslEngine.unwrap(rawIn, app);
					logger.debug("unwrap {}", r);
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
					handler.onHandshakeDone(sslEngine); // fallthrough
				default:
					return false;
			}
		}
	}

	@Override
	public ByteBuffer beforeWrite(ByteBuffer b) throws IOException {
		ByteBuffer out = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
		SSLEngineResult r = sslEngine.wrap(b, out);
		logger.debug("wrap {}", r);
		checkHandshake(r.getHandshakeStatus());
		return out.flip();
	}

	@Override
	public void onWrite(long now) throws IOException {
		if (processHandshake())
			return;
		handler.onWrite(now);
	}

	private boolean processHandshake() throws IOException {
		HandshakeStatus hs = sslEngine.getHandshakeStatus();
		while (true) {
			SSLEngineResult r;
			switch (hs) {
				case NEED_TASK:
					logger.debug("running tasks");
					co.submit(new RunTask());
					return true;
				case NEED_UNWRAP:
				case NEED_UNWRAP_AGAIN:
					rawIn.flip();
					r = sslEngine.unwrap(rawIn, app);
					logger.debug("unwrap {}", r);
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
					handler.onHandshakeDone(sslEngine); // fallthrough
				default:
					return false;
			}
		}
	}

	private final class RunTask implements Runnable, WorkerTask {
		@Override
		public void run() {
			logger.debug("start task");
			Runnable task;
			while ((task = sslEngine.getDelegatedTask()) != null)
				task.run();
			co.execute(this);
		}

		@Override
		public void run(long now) {
			logger.debug("resume handshake");
			try {
				processHandshake();
			} catch (Exception e) {
				logger.warn("Failed to handshake", e);
			}
		}
	}
}
