/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread responsible of all io
 * 
 * @author unknow
 */
public final class NIOWorker extends NIOLoop implements NIOWorkers {
	private static final Logger logger = LoggerFactory.getLogger(NIOWorker.class);

	/** this worker id */
	private final int id;
	private final NIOStaleWorker connections;
	/** the listener */
	private final NIOServerListener listener;

	/** buffer to read/write */
	private final ByteBuffer buf;

	private final Queue<SelectionKey> init;
	private final Queue<NIOConnectionAbstract> close;

	/**
	 * create new IOWorker
	 * 
	 * @param id the worker id
	 * @param connections connection worker
	 * @param listener listener to use
	 * @param timeout the timeout on select
	 * @throws IOException on ioexception
	 */
	public NIOWorker(int id, NIOStaleWorker connections, NIOServerListener listener, long timeout) throws IOException {
		super("NIOWorker-" + id, timeout);
		this.id = id;
		this.connections = connections;
		this.listener = listener;

		this.buf = ByteBuffer.allocateDirect(25000);

		this.init = new ConcurrentLinkedDeque<>();
		this.close = new ConcurrentLinkedDeque<>();
	}

	/**
	 * register a new socket to this thread
	 * 
	 * @param socket the socket to register
	 * @param pool the connection factory
	 * @throws IOException on ioexception
	 * @throws InterruptedException on interrupt
	 */
	@SuppressWarnings("resource")
	@Override
	public final void register(SocketChannel socket, BiFunction<NIOWorker, SelectionKey, NIOConnectionAbstract> pool) throws IOException, InterruptedException {
		socket.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE).configureBlocking(false);
		SelectionKey key = socket.register(selector, 0);
		NIOConnectionAbstract co = pool.apply(this, key);
		key.attach(co);
		init.add(key);
		selector.wakeup();
	}

	@Override
	protected final void selected(SelectionKey key, long now) throws IOException, InterruptedException {
		NIOConnectionAbstract co = (NIOConnectionAbstract) key.attachment();
		connections.add(co);

		if (key.isValid() && key.isWritable()) {
			try {
				co.lastAction = now;
				co.writeInto(buf);
			} catch (InterruptedException e) {
				logger.error("failed to write {}", co, e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.error("failed to write {}", co, e);
				close(key);
			} finally {
				buf.clear();
			}
		}

		if (key.isValid() && key.isReadable()) {
			try {
				co.lastAction = now;
				co.readFrom(buf);
			} catch (InterruptedException e) {
				logger.error("failed to read {}", co, e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.error("failed to read {}", co, e);
				close(key);
			} finally {
				buf.clear();
			}
		}
	}

	@Override
	protected void onSelect(boolean close) throws InterruptedException {
		doInit();
		doClose();
	}

	private final void doInit() throws InterruptedException {
		if (init.isEmpty())
			return;

		long now = System.currentTimeMillis();
		SelectionKey k;
		int i = 0;
		while (i++ < 100 && (k = init.poll()) != null) {
			k.interestOps(SelectionKey.OP_READ);
			NIOConnectionAbstract co = (NIOConnectionAbstract) k.attachment();
			listener.accepted(id, co);
			try {
				co.onInit();
				co.lastAction = now;
				connections.add(co);
			} catch (IOException e) {
				logger.warn("{} Failed to init", co, e);
				close(k);
			}
		}
		if (!init.isEmpty())
			selector.wakeup();
	}

	private final void doClose() {
		if (close.isEmpty())
			return;

		NIOConnectionAbstract co;
		int i = 0;
		while (i++ < 1000 && (co = close.poll()) != null)
			close(co.key);
	}

	protected final void close(NIOConnectionAbstract co) {
		close.offer(co);
	}

	@Override
	protected final void close(SelectionKey key) {
		NIOConnectionAbstract co = (NIOConnectionAbstract) key.attachment();
		connections.remove(co);
		listener.closed(id, co);

		key.cancel();
		try {
			key.channel().close();
		} catch (@SuppressWarnings("unused") IOException e) { // ignore
		}
		try {
			co.free();
		} catch (Exception e) {
			logger.warn("Failed to free connection {}", co, e);
		}
	}
}