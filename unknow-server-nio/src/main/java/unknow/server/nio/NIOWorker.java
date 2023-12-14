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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

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

	/** used for all synchronization */
	private final Lock mutex;
	/** buffer to read/write */
	private final ByteBuffer buf;

	private final Queue<SelectionKey> init;

	/**
	 * create new IOWorker
	 * 
	 * @param id       the worker id
	 * @param listener listener to use
	 * @param timeout  the timeout on select
	 * @throws IOException
	 */
	public NIOWorker(int id, NIOServerListener listener, long timeout) throws IOException {
		super("NIOWorker-" + id, timeout);
		this.id = id;
		this.listener = listener;

		this.mutex = new ReentrantLock();
		this.buf = ByteBuffer.allocateDirect(4096);
		this.init = new ArrayDeque<>();
	}

	/**
	 * register a new socket to this thread
	 * 
	 * @param socket  the socket to register
	 * @param pool the connection factory
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("resource")
	@Override
	public final void register(SocketChannel socket, Supplier<NIOConnection> pool) throws IOException, InterruptedException {
		socket.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE).configureBlocking(false);
		mutex.lockInterruptibly();
		try {
			selector.wakeup();
			init.add(socket.register(selector, SelectionKey.OP_READ, pool.get()));
		} finally {
			mutex.unlock();
		}
	}

	@Override
	@SuppressWarnings("resource")
	protected final void selected(SelectionKey key) throws IOException, InterruptedException {
		NIOConnection h = (NIOConnection) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();

		if (key.isValid() && key.isWritable()) {
			try {
				h.writeInto(channel, buf);
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
				h.readFrom(channel, buf);
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
		mutex.lockInterruptibly();
		try {
			SelectionKey k;
			while ((k = init.poll()) != null) {
				NIOConnection co = (NIOConnection) k.attachment();
				co.init(k);
				listener.accepted(id, co);
			}

			long now = System.currentTimeMillis();
			for (SelectionKey key : selector.keys()) {
				NIOConnection co = (NIOConnection) key.attachment();
				if (!key.isValid() || co.closed(now, close)) {
					listener.closed(id, co);
					key.cancel();
					try {
						key.channel().close();
					} catch (@SuppressWarnings("unused") IOException e) { // ignore
					}
					try {
						co.free();
					} catch (InterruptedException | IOException e) {
						logger.warn("Failed to free connection {}", co, e);
					}
				}
			}
		} finally {
			mutex.unlock();
		}
	}
}