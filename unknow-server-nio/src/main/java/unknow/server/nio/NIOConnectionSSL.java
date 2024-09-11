package unknow.server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NIOConnectionSSL extends NIOConnection {
	private static final Logger logger = LoggerFactory.getLogger(NIOConnectionSSL.class);

	protected final SSLEngine sslEngine;

	private final ByteBuffer rawIn;
	private final ByteBuffer rawOut;
	private final ByteBuffer app;

	private Future<?> handshake = CompletableFuture.completedFuture(null);

	public NIOConnectionSSL(SelectionKey key, SSLContext sslContext) {
		super(key);
		this.sslEngine = sslContext.createSSLEngine(getRemote().getHostString(), getRemote().getPort());
		this.rawIn = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
		this.rawOut = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
		this.app = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize());

		rawOut.flip();
	}

	@Override
	public boolean closed(long now, boolean stop) {
		if (sslEngine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING)
			return false;
		return handshake.isDone() && isClosed();
	}

	@Override
	protected final void onInit() throws InterruptedException {
		onInit(sslEngine);
		try {
			sslEngine.beginHandshake();
			processHandshake();
		} catch (IOException e) {
			logger.error("Failed to start connection", e);
			key.cancel();
		}
	}

	protected void onInit(@SuppressWarnings("unused") SSLEngine sslEngine) { // for override
	}

	/**
	 * called when the handshake process finish
	 * @throws InterruptedException on interrupt
	 */
	protected void onHandshakeDone() throws InterruptedException { // for override
	}

	@Override
	protected void onRead() throws InterruptedException, IOException {
		logger.info("{}", pendingRead());
	}

	@Override
	protected void readFrom(ByteBuffer buf) throws InterruptedException, IOException {
		lastRead = System.currentTimeMillis();
		if (processHandshake())
			return;
		int l;
		while (true) {
			l = channel.read(rawIn);
			if (l == -1) {
				in.close();
				break;
			}
			if (l == 0 && rawIn.position() == 0)
				break;
			rawIn.flip();
			SSLEngineResult r = sslEngine.unwrap(rawIn, app);
			logger.debug("unwrap {}", r.getStatus());
			rawIn.compact();

			app.flip();
			pendingRead().write(app);
			app.compact();
		}
		onRead();
	}

	@Override
	protected void writeInto(ByteBuffer buf) throws InterruptedException, IOException {
		lastWrite = System.currentTimeMillis();
		if (rawOut.remaining() > 0) {
			channel.write(rawOut);
			return;
		}

		if (processHandshake())
			return;

		while (pendingWrite().read(app, false)) {
			app.flip();
			rawOut.compact();
			sslEngine.wrap(app, rawOut);
			app.compact();
			rawOut.flip();

			channel.write(rawOut);
			if (rawOut.hasRemaining()) {
				if (app.position() > 0) {
					app.flip();
					pendingWrite().prepend(app);
					app.compact();
				}
				break;
			}
		}
		if (app.position() > 0) {
			app.flip();
			pendingWrite().prepend(app);
			app.compact();
		}
		toggleKeyOps();
		onWrite();
	}

	private boolean processHandshake() throws IOException, InterruptedException {
		if (!handshake.isDone())
			return true;

		HandshakeStatus hs = sslEngine.getHandshakeStatus();
		while (true) {
			SSLEngineResult r;
			switch (hs) {
				case NEED_TASK:
					logger.debug("running tasks");
					Runnable task;
					while ((task = sslEngine.getDelegatedTask()) != null)
						task.run();
					hs = sslEngine.getHandshakeStatus();
					break;
				case NEED_UNWRAP:
				case NEED_UNWRAP_AGAIN:
					if (channel.read(rawIn) < 0)
						throw new IOException("connection reset by peer");
					rawIn.flip();
					r = sslEngine.unwrap(rawIn, app);
					logger.debug("unwrap {}", r);
					rawIn.compact();
					if (r.getStatus() == Status.BUFFER_UNDERFLOW) {
						key.interestOps(SelectionKey.OP_READ);
						return true;
					}
					hs = r.getHandshakeStatus();
					break;
				case NEED_WRAP:
					rawOut.compact();
					r = sslEngine.wrap(app, rawOut);
					logger.debug("wrap {}", r);
					rawOut.flip();
					// TODO check status
					channel.write(rawOut);
					if (rawOut.remaining() > 0) {
						key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
						return true;
					}
					hs = r.getHandshakeStatus();
					break;
				case FINISHED:
					toggleKeyOps();
					onHandshakeDone(); // fallthrough
				default:
					return false;
			}
		}
	}
}
