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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread responsible of all io
 * 
 * @author unknow
 */
public final class NIOWorker extends NIOLoop implements NIOWorkers {
	private static final Logger log = LoggerFactory.getLogger(NIOWorker.class);

	/** this worker id */
	private final int id;
	/** the listener */
	private final NIOServerListener listener;

	/** used for all synchronization */
	private final Object mutex;
	/** buffer to read/write */
	private final ByteBuffer buf;

	private final Queue<Connection> init;

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

		this.mutex = new Object();
		this.buf = ByteBuffer.allocateDirect(4096);
		this.init = new ConcurrentLinkedQueue<>();
	}

	/**
	 * register a new socket to this thread
	 * 
	 * @param socket  the socket to register
	 * @param handler the handler
	 * @throws IOException
	 */
	@Override
	public final void register(SocketChannel socket, Connection handler) throws IOException {
		socket.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE).setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE).configureBlocking(false);
		synchronized (mutex) {
			selector.wakeup();
			init.add(handler);
			handler.attach(socket.register(selector, SelectionKey.OP_READ, handler));
		}
		listener.accepted(id, handler);
	}

	@Override
	protected final void selected(SelectionKey key) throws IOException, InterruptedException {
		Connection h = (Connection) key.attachment();
		SocketChannel channel = (SocketChannel) key.channel();

		if (key.isValid() && key.isWritable()) {
			try {
				h.writeInto(channel, buf);
			} catch (IOException e) {
				log.error("failed to write", h, e);
				channel.close();
			} finally {
				buf.clear();
			}
		}

		// Tests whether this key's channel is ready to accept a new socket connection
		if (key.isValid() && key.isReadable()) {
			try {
				h.readFrom(channel, buf);
			} catch (IOException e) {
				log.error("failed to read {}", h, e);
				channel.close();
			} finally {
				buf.clear();
			}
		}
	}

	@Override
	protected void onSelect(boolean close) throws InterruptedException {
		synchronized (mutex) {
			Connection co;
			while ((co = init.poll()) != null)
				co.init();

			long now = System.currentTimeMillis();
			for (SelectionKey key : selector.keys()) {
				co = (Connection) key.attachment();
				if (!key.isValid() || co.closed(now, close)) {
					listener.closed(id, co);
					key.cancel();
					try {
						key.channel().close();
						co.free();
					} catch (@SuppressWarnings("unused") IOException e) { // ignore
					}
				}
			}
		}
	}
}