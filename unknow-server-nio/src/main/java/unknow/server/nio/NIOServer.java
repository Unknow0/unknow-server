/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Function;

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
	/** the listener */
	private final NIOServerListener listener;

	/**
	 * create new Server
	 * 
	 * @param workers  workers handler
	 * @param listener listener
	 * 
	 * @throws IOException on ioException
	 */
	public NIOServer(NIOWorkers workers, NIOServerListener listener) throws IOException {
		super("NIOServer", 0);
		this.workers = workers;
		this.listener = listener == null ? NIOServerListener.NOP : listener;
	}

	/**
	 * bind the server on an address
	 * 
	 * @param a the address to bind to
	 * @param s factory for the connection 
	 * @throws IOException on ioException
	 */
	@SuppressWarnings("resource")
	public void bind(SocketAddress a, Function<SelectionKey, NIOConnectionAbstract> s) throws IOException {
		logger.info("Server bind to {}", a);
		ServerSocketChannel open = ServerSocketChannel.open();
		open.configureBlocking(false);
		open.register(selector, SelectionKey.OP_ACCEPT, s);
		open.bind(a);
	}

	@Override
	protected void onStartup() {
		workers.start();
		listener.starting(this);
	}

	@SuppressWarnings("resource")
	@Override
	protected void selected(SelectionKey key) throws IOException, InterruptedException {
		try {
			@SuppressWarnings("unchecked")
			Function<SelectionKey, NIOConnectionAbstract> factory = (Function<SelectionKey, NIOConnectionAbstract>) key.attachment();
			SocketChannel socket = ((ServerSocketChannel) key.channel()).accept();
			workers.register(socket, factory);
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
