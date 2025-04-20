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

	private final Queue<Runnable> tasks;

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
		this.tasks = new ArrayDeque<>();

	}

	/**
	 * register a new socket to this thread
	 * 
	 * @param socket the socket to register
	 * @param pool the connection factory
	 * @throws IOException on ioexception
	 * @throws InterruptedException on interrupt
	 */
	@Override
	public final void register(SocketChannel socket, Function<SelectionKey, NIOConnectionAbstract> pool) throws IOException, InterruptedException {
		tasks.add(new RegisterTask(socket, pool));
		selector.wakeup();
	}

	@Override
	protected final void selected(SelectionKey key) throws IOException, InterruptedException {
		NIOConnectionAbstract co = (NIOConnectionAbstract) key.attachment();

		if (key.isValid() && key.isWritable()) {
			try {
				co.writeInto(buf);
				toTail(co);
			} catch (InterruptedException e) {
				logger.error("failed to write {}", co, e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.error("failed to write {}", co, e);
				close(co);
			} finally {
				buf.clear();
			}
		}

		// Tests whether this key's channel is ready to accept a new socket connection
		if (key.isValid() && key.isReadable()) {
			try {
				if (co.readFrom(buf))
					toTail(co);
			} catch (InterruptedException e) {
				logger.error("failed to read {}", co, e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				logger.error("failed to read {}", co, e);
				close(co);
			} finally {
				buf.clear();
			}
		}
	}

	@Override
	protected void onSelect(boolean close) throws InterruptedException {
		Runnable r;
		while ((r = tasks.poll()) != null)
			r.run();

		if (head == null)
			return;

		long now = System.currentTimeMillis();
		long end = now - 1000;
		NIOConnectionAbstract co = head;
		while (co != null && co.lastCheck < end) {
			if (!co.key.isValid() || co.closed(now, close))
				close(co);
			else
				co.lastCheck = now;
			co = co.next;
		}

		// move all to tail
		if (co != null && co.prev != null) {
			tail.next = head;
			head.prev = tail;

			tail = co.prev;
			tail.next = null;

			co.prev = null;
			head = co;
		}
	}

	private void close(NIOConnectionAbstract co) {
		listener.closed(id, co);
		try {
			co.free();
		} catch (Exception e) {
			logger.warn("Failed to free connection {}", co, e);
		}
		remove(co);
	}

	private final class RegisterTask implements Runnable {
		private final SocketChannel socket;
		private final Function<SelectionKey, NIOConnectionAbstract> pool;

		public RegisterTask(SocketChannel socket, Function<SelectionKey, NIOConnectionAbstract> pool) {
			this.socket = socket;
			this.pool = pool;
		}

		@SuppressWarnings("resource")
		@Override
		public void run() {
			SelectionKey key = null;
			try {
				socket.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE).setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE).configureBlocking(false);
				key = socket.register(selector, SelectionKey.OP_READ);
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException ex) {
					e.addSuppressed(ex);
				}
				logger.warn("Failed to register socket", e);
				return;
			}
			NIOConnectionAbstract co = pool.apply(key);
			listener.accepted(id, co);
			key.attach(co);
			toTail(co);
			try {
				co.onInit();
			} catch (@SuppressWarnings("unused") InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private final void remove(NIOConnectionAbstract co) {
		if (co.prev != null)
			co.prev.next = co.next;
		if (co.next != null)
			co.next.prev = co.prev;

		if (head == co)
			head = co.next;
		if (tail == co)
			tail = co.prev;
	}

	private final void toTail(NIOConnectionAbstract co) {
		co.lastCheck = System.currentTimeMillis();
		if (tail == co)
			return;
		if (co.prev != null)
			co.prev.next = co.next;
		if (co.next != null)
			co.next.prev = co.prev;
		co.next = null;
		co.prev = tail;
		if (tail != null)
			tail.next = co;
		tail = co;
		if (head == null)
			head = co;
	}
}