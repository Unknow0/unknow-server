/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
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
	private final Deque<SelectionKey> keys;
	private final AtomicBoolean wakeup = new AtomicBoolean(true);

	private long lastCheck;

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
		this.init = new ConcurrentLinkedDeque<>();

		this.keys = new LinkedList<>();
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
		if (wakeup.compareAndSet(true, false))
			selector.wakeup();
	}

	@Override
	protected final void selected(SelectionKey key, long now) throws IOException, InterruptedException {
		NIOConnectionAbstract h = (NIOConnectionAbstract) key.attachment();
		keys.remove(key);
		keys.addLast(key);
		if (key.isValid() && key.isWritable()) {
			try {
				h.writeInto(buf, now);
			} catch (InterruptedException e) {
				logger.error("failed to write {}", h, e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.error("failed to write {}", h, e);
				close(key);
			} finally {
				buf.clear();
			}
		}

		// Tests whether this key's channel is ready to accept a new socket connection
		if (key.isValid() && key.isReadable()) {
			try {
				h.readFrom(buf, now);
			} catch (InterruptedException e) {
				logger.error("failed to read {}", h, e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.error("failed to read {}", h, e);
				close(key);
			} finally {
				buf.clear();
			}
		}
	}

	@Override
	protected void onSelect(boolean close) throws InterruptedException {
		SelectionKey k;
		int i = 0;
		long now = System.currentTimeMillis();
		while (i++ < 100 && (k = init.poll()) != null) {
			k.interestOps(SelectionKey.OP_READ);
			keys.addLast(k);
			NIOConnectionAbstract co = (NIOConnectionAbstract) k.attachment();
			listener.accepted(id, co);
			try {
				co.onInit(now);
			} catch (IOException e) {
				logger.warn("{} Failed to init", co, e);
				close(k);
			}
		}
		if (!init.isEmpty())
			selector.wakeup();
		else
			wakeup.set(true);

		if (now - lastCheck < 5000)
			return;
		lastCheck = now;

		long e = now - 5000;
		Iterator<SelectionKey> it = keys.iterator();
		while (it.hasNext()) {
			SelectionKey next = it.next();
			NIOConnectionAbstract co = (NIOConnectionAbstract) next.attachment();
			if (co.lastRead() > e || co.lastWrite > e)
				break;
			if (!next.isValid() || co.closed(now, close)) {
				it.remove();
				close(next);
			}
		}
	}

	@Override
	protected void close(SelectionKey key) {
		NIOConnectionAbstract co = (NIOConnectionAbstract) key.attachment();
		listener.closed(id, co);
		keys.remove(key);
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