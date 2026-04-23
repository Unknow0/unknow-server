/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the NIO Server
 * 
 * @author unknow
 */
public class NIOServer extends NIOLoop {
	private static final Logger logger = LoggerFactory.getLogger(NIOServer.class);

	/** the workers to handle the connection */
	private final NIOWorkers workers;

	/**
	 * create new Server
	 * 
	 * @param workers workers handler
	 * @param listener listener
	 * 
	 * @throws IOException on ioException
	 */
	public NIOServer(NIOWorkers workers, NIOServerListener listener) throws IOException {
		super("NIOServer", 0, listener == null ? NIOServerListener.NOP : listener);
		this.workers = workers;
	}

	protected Collection<NIOWorker> workers() {
		return workers.workers();
	}

	/**
	 * bind the server on an address
	 * 
	 * @param a the address to bind to
	 * @param s factory for the connection
	 * @throws IOException on ioException
	 */
	@SuppressWarnings("resource")
	public void bind(SocketAddress a, ConnectionFactory s) throws IOException {
		logger.info("Server bind to {}", a);
		ServerSocketChannel open = ServerSocketChannel.open();
		open.configureBlocking(false).register(selector, SelectionKey.OP_ACCEPT, s);
		open.bind(a, Integer.MAX_VALUE);
	}

	@Override
	protected void onStartup() {
		workers.start();
		listener.starting(this);
	}

	@SuppressWarnings("resource")
	@Override
	protected void selected(long now, SelectionKey key) throws IOException, InterruptedException {
		ConnectionFactory factory = (ConnectionFactory) key.attachment();
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel socket = null;
		while ((socket = ssc.accept()) != null) {
			try {
				workers.register(socket, factory);
			} catch (IOException e) {
				try {
					socket.close();
				} catch (IOException ex) {
					e.addSuppressed(ex);
				}
				logger.warn("Failed to accept", e);
			}
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

	public static interface ConnectionFactory {
		NIOConnectionHandler build();
	}
}
