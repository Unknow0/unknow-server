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
import java.util.concurrent.ConcurrentLinkedQueue;
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
	private NIOConnectionAbstract head;
	private NIOConnectionAbstract tail;

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
		key.attach(pool.apply(key));
		init.add(key);
		selector.wakeup();
	}

	/**
	 * remove co from the chain
	 * @param co co to unlink
	 */
	private void unlink(NIOConnectionAbstract co) {
		if (co.prev != null)
			co.prev.next = co.next;
		if (co.next != null)
			co.next.prev = co.prev;
		if (co == head)
			head = co.next;
		if (co == tail)
			tail = co.prev;
	}

	/**
	 * move the co to the end of the chain
	 * @param co co to move
	 */
	private void move(NIOConnectionAbstract co) {
		unlink(co);
		co.prev = tail;
		co.next = null;
		tail = co;
		if (head == null)
			head = co;
	}

	/**
	 * remove co from the chain
	 * @param co co to remove
	 */
	private void remove(NIOConnectionAbstract co) {
		unlink(co);
		co.next = null;
		co.prev = null;
	}

	@Override
	protected final void selected(SelectionKey key) throws IOException, InterruptedException {
		NIOConnectionAbstract co = (NIOConnectionAbstract) key.attachment();
		move(co);

		if (key.isValid() && key.isWritable()) {
			try {
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

		// Tests whether this key's channel is ready to accept a new socket connection
		if (key.isValid() && key.isReadable()) {
			try {
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
	protected void close(SelectionKey key) {
		NIOConnectionAbstract co = (NIOConnectionAbstract) key.attachment();
		remove(co);
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

	@Override
	protected void onSelect(boolean close) throws InterruptedException {
		SelectionKey k;
		while ((k = init.poll()) != null) {
			k.interestOps(SelectionKey.OP_READ);
			NIOConnectionAbstract co = (NIOConnectionAbstract) k.attachment();
			listener.accepted(id, co);
			co.onInit();
			move(co);
		}
		checkConnections(close);
	}

	private final void checkConnections(boolean close) {
		long now = System.currentTimeMillis();
		NIOConnectionAbstract co = head;
		while (co != null) {
			if (!co.key.isValid() || co.closed(now, close)) {
				SelectionKey key = co.key;
				co = co.next;
				close(key);
			} else
				co = co.next;
		}
	}
}