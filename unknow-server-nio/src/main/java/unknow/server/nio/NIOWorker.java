/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

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
	/** the listener */
	private final NIOServerListener listener;

	/** buffer to read/write */
	private final ByteBuffer buf;

	private final Queue<SelectionKey> init;
	private final Queue<NIOConnectionAbstract> connections;

	/**
	 * create new IOWorker
	 * 
	 * @param id the worker id
	 * @param listener listener to use
	 * @param timeout the timeout on select
	 * @throws IOException on ioexception
	 */
	public NIOWorker(int id, NIOServerListener listener, long timeout) throws IOException {
		super("NIOWorker-" + id, timeout);
		this.id = id;
		this.listener = listener;

		this.buf = ByteBuffer.allocateDirect(25000);

		this.init = new ConcurrentLinkedQueue<>();
		this.connections = new ConcurrentLinkedQueue<>();
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
	public final void register(SocketChannel socket, Function<SelectionKey, NIOConnectionAbstract> pool) throws IOException, InterruptedException {
		socket.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE).configureBlocking(false);
		SelectionKey key = socket.register(selector, 0);
		init.add(key);
		key.attach(pool.apply(key));
		selector.wakeup();
	}

	@Override
	@SuppressWarnings("resource")
	protected final void selected(SelectionKey key) throws IOException, InterruptedException {
		NIOConnectionAbstract h = (NIOConnectionAbstract) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();

		connections.remove(h);
		connections.add(h);

		if (key.isValid() && key.isWritable()) {
			try {
				h.writeInto(buf);
			} catch (InterruptedException e) {
				logger.error("failed to write {}", h, e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.error("failed to write {}", h, e);
				channel.close();
			} finally {
				buf.clear();
			}
		}

		// Tests whether this key's channel is ready to accept a new socket connection
		if (key.isValid() && key.isReadable()) {
			try {
				h.readFrom(buf);
			} catch (InterruptedException e) {
				logger.error("failed to read {}", h, e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.error("failed to read {}", h, e);
				channel.close();
			} finally {
				buf.clear();
			}
		}
	}

	@Override
	protected void onSelect(boolean close) throws InterruptedException {
		SelectionKey k;
		while ((k = init.poll()) != null) {
			k.interestOps(SelectionKey.OP_READ);
			NIOConnectionAbstract co = (NIOConnectionAbstract) k.attachment();
			listener.accepted(id, co);
			co.onInit();
		}
	}

	private final void checkConnections(boolean close) throws InterruptedException {
		NIOConnectionAbstract first = connections.poll();
		if (first == null)
			return;

		long now = System.currentTimeMillis();
		int i = 0;
		NIOConnectionAbstract co = first;
		do {
			if (!co.key.isValid() || co.closed(now, close))
				co.close();
			else
				connections.put(co);
			co = connections.poll();
		} while (i++ < 1000 && co != null && first != co);
	}
}