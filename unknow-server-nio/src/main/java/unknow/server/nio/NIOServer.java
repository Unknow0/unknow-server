/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.util.pool.Pool;
import unknow.server.util.pool.SharedPool;

/**
 * the NIO Server
 * 
 * @author unknow
 */
public class NIOServer extends NIOLoop {
	private static final Logger logger = LoggerFactory.getLogger(NIOServer.class);

	/** the workers to handle the connection */
	private final NIOWorkers workers;
	/** the listener */
	private final NIOServerListener listener;

	private final Map<Function<Pool<NIOConnection>, ? extends NIOConnection>, Pool<NIOConnection>> pools;

	/**
	 * create new Server
	 * 
	 * @param workers  workers handler
	 * @param listener listener
	 * 
	 * @throws IOException
	 */
	public NIOServer(NIOWorkers workers, NIOServerListener listener) throws IOException {
		super("NIOServer", 0);
		this.workers = workers;
		this.listener = listener == null ? NIOServerListener.NOP : listener;
		this.pools = new HashMap<>();
	}

	/**
	 * bind the server on an address
	 * 
	 * @param a
	 * @param handler
	 * @throws IOException
	 */
	@SuppressWarnings("resource")
	public void bind(SocketAddress a, Function<Pool<NIOConnection>, ? extends NIOConnection> s) throws IOException {
		logger.info("Server bind to {}", a);
		Pool<NIOConnection> pool;
		synchronized (pools) {
			pool = pools.computeIfAbsent(s, k -> new SharedPool<>(200, s));
		}

		ServerSocketChannel open = ServerSocketChannel.open();
		open.configureBlocking(false);
		open.register(selector, SelectionKey.OP_ACCEPT, pool);
		open.bind(a);
	}

	@Override
	protected void onStartup() {
		workers.start();
		listener.starting(this);
	}

	@SuppressWarnings("resource")
	@Override
	protected void selected(SelectionKey key) throws IOException {
		try {
			@SuppressWarnings("unchecked")
			Pool<NIOConnection> pool = (Pool<NIOConnection>) key.attachment();
			SocketChannel socket = ((ServerSocketChannel) key.channel()).accept();
			workers.register(socket, pool.get());
		} catch (IOException e) {
			logger.warn("Failed to accept", e);
		}
	}

	@Override
	protected void beforeStop() {
		workers.stop();
		workers.await();
		try {
			selector.close();
		} catch (IOException e) {
			logger.error("failed to close selecctor", e);
		}
	}

	@Override
	protected void afterStop() {
		listener.closing(this, null);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("NIOServer[");
		// TODO
		return sb.append(']').toString();
	}
}
