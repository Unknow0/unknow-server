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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOServer.ConnectionFactory;

/**
 * Thread responsible of all io
 * 
 * @author unknow
 */
public final class NIOWorker extends NIOLoop implements NIOWorkers {
	private static final Logger logger = LoggerFactory.getLogger(NIOWorker.class);

	/** this worker id */
	private final int id;
	/** executor for delegating task */
	private final ExecutorService executor;
	/** the listener */
	private final NIOServerListener listener;

	/** buffer to read/write */
	private final ByteBuffer buf;

	private final Queue<WorkerTask> tasks;

	private NIOConnectionAbstract head;
	private NIOConnectionAbstract tail;

	/**
	 * create new IOWorker
	 * 
	 * @param id the worker id
	 * @param executor executor for delegated task
	 * @param listener listener to use
	 * @param timeout the timeout on select
	 * @throws IOException on ioexception
	 */
	public NIOWorker(int id, ExecutorService executor, NIOServerListener listener, long timeout) throws IOException {
		super("NIOWorker-" + id, timeout);
		this.id = id;
		this.executor = executor;
		this.listener = listener;

		this.buf = ByteBuffer.allocateDirect(25000);
		this.tasks = new ConcurrentLinkedQueue<>();
	}

	protected final void execute(WorkerTask task) {
		tasks.add(task);
		selector.wakeup();
	}

	/**
	 * register a new socket to this thread
	 * 
	 * @param socket the socket to register
	 * @param factory the connection factory
	 */
	@Override
	public final void register(SocketChannel socket, ConnectionFactory factory) {
		execute(new RegisterTask(socket, factory));
	}

	@Override
	protected final void selected(long now, SelectionKey key) throws IOException, InterruptedException {
		NIOConnectionAbstract co = (NIOConnectionAbstract) key.attachment();

		if (key.isValid() && key.isWritable()) {
			try {
				co.writeInto(buf, now);
				toTail(co, now);
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
				if (co.readFrom(buf, now))
					toTail(co, now);
				else if (!co.key.isValid() || co.closed(now, false))
					close(co);
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
	protected void onSelect(long now, boolean close) throws InterruptedException {
		WorkerTask r;
		while ((r = tasks.poll()) != null)
			r.run(now);

		if (head == null)
			return;

		long end = now - 1000;
		NIOConnectionAbstract co = head;
		while (co != null && co.lastCheck < end) {
			NIOConnectionAbstract next = co.next;
			if (!co.key.isValid() || co.closed(now, close))
				close(co);
			else
				co.lastCheck = now;
			co = next;
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

	@SuppressWarnings("unchecked")
	public final <T> Future<T> submit(Runnable r) {
		return (Future<T>) executor.submit(r);
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

	public static interface WorkerTask {
		void run(long now);
	}

	private final void unlink(NIOConnectionAbstract co) {
		if (co.prev != null)
			co.prev.next = co.next;
		if (co.next != null)
			co.next.prev = co.prev;

		if (head == co)
			head = co.next;
		if (tail == co)
			tail = co.prev;
	}

	private final void remove(NIOConnectionAbstract co) {
		unlink(co);
		co.next = co.prev = null;
	}

	private final void toTail(NIOConnectionAbstract co, long now) {
		co.lastCheck = now;

		if (tail == co)
			return;
		unlink(co);

		co.next = null;
		co.prev = tail;

		if (tail != null)
			tail.next = co;
		tail = co;
		if (head == null)
			head = co;
	}

	private final class RegisterTask implements WorkerTask {
		private final SocketChannel socket;
		private final ConnectionFactory pool;

		public RegisterTask(SocketChannel socket, ConnectionFactory pool) {
			this.socket = socket;
			this.pool = pool;
		}

		@SuppressWarnings("resource")
		@Override
		public void run(long now) {
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
			NIOConnectionAbstract co = pool.build(NIOWorker.this, key, now);
			listener.accepted(id, co);
			key.attach(co);
			toTail(co, now);
			try {
				co.onInit();
			} catch (Exception e) {
				logger.warn("Failed to start connection", e);
				close(co);
			}
		}
	}
}