/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

	/** executor for delegating task */
	private final ExecutorService executor;

	/** buffer to read/write */
	private final ByteBuffer buf;

	private final Queue<WorkerTask> tasks;

	private final List<NIOConnection> closing;

	private NIOConnection head;
	private NIOConnection tail;

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
		super("NIOWorker-" + id, timeout, listener);
		this.executor = executor;

		this.buf = ByteBuffer.allocateDirect(16000);
		this.tasks = new ConcurrentLinkedQueue<>();
		this.closing = new LinkedList<>();
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
	protected void onSelect(long now, boolean close) {
		WorkerTask r;
		while ((r = tasks.poll()) != null)
			r.run(now);

		checkPending(now, close);
		finishClosing(now);
	}

	private void checkPending(long now, boolean close) {
		if (head == null)
			return;
		long end = now - 1_000_000_000L;
		NIOConnection co = head;
		while (co != null && co.lastCheck < end) {
			NIOConnection next = co.next;
			if (!co.key.isValid() || co.canClose(now, close))
				startClose(co);
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

	private void finishClosing(long now) {
		if (closing.isEmpty())
			return;

		Iterator<NIOConnection> it = closing.iterator();
		while (it.hasNext()) {
			NIOConnection co = it.next();
			if (!co.key.isValid() || !co.hasPendingWrites() && co.finishClosing(now)) {
				it.remove();
				doneClose(co);
			}
		}
	}

	@Override
	protected final void selected(long now, SelectionKey key) throws IOException {
		NIOConnection co = (NIOConnection) key.attachment();

		if (key.isValid() && key.isWritable()) {
			try {
				doWrite(co, now);
			} catch (Exception e) {
				logger.error("failed to write {}", co, e);
				startClose(co);
			} finally {
				buf.clear();
			}
		}

		// Tests whether this key's channel is ready to accept a new socket connection
		if (key.isValid() && key.isReadable()) {
			try {
				doRead(co, now);
			} catch (Exception e) {
				logger.error("failed to read {}", co, e);
				startClose(co);
			} finally {
				buf.clear();
			}
		}
	}

	private void doRead(NIOConnection co, long now) throws IOException {
		int l;
		l = co.channel.read(buf);
		if (l == -1) {
			co.key.interestOpsAnd(~SelectionKey.OP_READ);
			startClose(co);
			return;
		}
		if (l == 0) {
			toTail(co, now);
			return;
		}
		buf.flip();
		ByteBuffer data = ByteBuffer.allocate(buf.remaining());
		data.put(buf);
		data.flip();
		co.onRead(data, now);
	}

	private void doWrite(NIOConnection co, long now) throws IOException {
		co.beforeWrite(now);
		if (co.channel.write(co.writes.buf, 0, co.writes.len) > 0) {
			co.onWrite(now);
			toTail(co, now);
		}
	}

	@SuppressWarnings("unchecked")
	public final <T> Future<T> submit(Runnable r) {
		return (Future<T>) executor.submit(r);
	}

	private void startClose(NIOConnection co) {
		if (remove(co))
			return;
		logger.debug("{} start closing", co);
		closing.add(co);
		co.startClose();
	}

	private void doneClose(NIOConnection co) {
		listener.closed(name, co);
		try {
			co.doneClosing();
		} catch (Exception e) {
			logger.warn("Failed to free connection {}", co, e);
		}
	}

	public static interface WorkerTask {
		void run(long now);
	}

	private final void unlink(NIOConnection co) {
		if (co.prev != null)
			co.prev.next = co.next;
		if (co.next != null)
			co.next.prev = co.prev;

		if (head == co)
			head = co.next;
		if (tail == co)
			tail = co.prev;
	}

	private final boolean remove(NIOConnection co) {
		if (co.next == co && co.prev == co)
			return true;
		unlink(co);
		co.next = co.prev = co;
		return false;
	}

	private final void toTail(NIOConnection co, long now) {
		co.lastCheck = now;

		if (tail == co || co.next == co)
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
		private final ConnectionFactory factory;

		public RegisterTask(SocketChannel socket, ConnectionFactory factory) {
			this.socket = socket;
			this.factory = factory;
		}

		@SuppressWarnings("resource")
		@Override
		public void run(long now) {
			SelectionKey key = null;
			try {
				socket.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE).setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE).configureBlocking(false);
				socket.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024).setOption(StandardSocketOptions.SO_SNDBUF, 64 * 1024);
				key = socket.register(selector, 0);
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException ex) {
					e.addSuppressed(ex);
				}
				logger.warn("Failed to register socket", e);
				return;
			}
			NIOConnection co = new NIOConnection(NIOWorker.this, key, factory.build());
			key.attach(co);
			listener.accepted(name, co);
			if (!co.asyncInit()) {
				try {
					co.init(co, now, null);
					key.interestOps(SelectionKey.OP_READ);
				} catch (Exception e) {
					logger.warn("Failed to init connection", e);
					startClose(co);
					return;
				}
			} else
				submit(new AsyncInit(co));
			toTail(co, now);
		}
	}

	private static final class AsyncInit implements Runnable {
		private final NIOConnection co;

		public AsyncInit(NIOConnection co) {
			this.co = co;
		}

		@SuppressWarnings("resource")
		@Override
		public void run() {
			try {
				co.init(co, System.nanoTime(), null);
				co.key.interestOps(SelectionKey.OP_READ);
				co.key.selector().wakeup();
			} catch (Exception e) {
				logger.warn("Failed to init connection", e);
				co.key.cancel();
			}
		}
	}
}