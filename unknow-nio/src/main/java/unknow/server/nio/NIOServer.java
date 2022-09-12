/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the NIO Server
 * 
 * @author unknow
 */
public class NIOServer extends NIOLoop {
	private static final Logger log = LoggerFactory.getLogger(NIOServer.class);

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
	 * @throws IOException
	 */
	public NIOServer(NIOWorkers workers, NIOServerListener listener) throws IOException {
		super("NIOServer", 0);
		this.workers = workers;
		this.listener = listener == null ? NIOServerListener.NOP : listener;
	}

	/**
	 * bind the server on an address
	 * 
	 * @param a
	 * @param handler
	 * @throws IOException
	 */
	public void bind(SocketAddress a, HandlerFactory handler) throws IOException {
		log.info("Server bind to {}", a);
		ServerSocketChannel open = ServerSocketChannel.open();
		open.configureBlocking(false);
		open.register(selector, SelectionKey.OP_ACCEPT, handler);
		open.bind(a);
	}

	@Override
	protected void onStartup() {
		workers.start();
		listener.starting(this);
	}

	@Override
	protected void selected(SelectionKey key) throws IOException {
		try {
			HandlerFactory factory = (HandlerFactory) key.attachment();
			SocketChannel socket = ((ServerSocketChannel) key.channel()).accept();
			workers.register(socket, new Connection(factory));
		} catch (IOException e) {
			log.warn("Failed to accept", e);
		}
	}

	@Override
	protected void beforeStop() {
		workers.stop();
		workers.await();
		try {
			selector.close();
		} catch (IOException e) {
			log.error("failed to close selecctor", e);
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
