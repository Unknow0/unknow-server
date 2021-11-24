/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the NIO Server
 * 
 * @author unknow
 */
public class NIOServer implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(NIOServer.class);
	private final NIOServerListener listener;
	private final ServerSocketChannel channel;

	private final NIOWorkers workers;

	/**
	 * create new Server
	 * 
	 * @param bindTo   address to bind to
	 * @param workers  workers handler
	 * @param listener listener
	 * 
	 * @throws IOException
	 */
	public NIOServer(SocketAddress bindTo, NIOWorkers workers, NIOServerListener listener) throws IOException {
		log.debug("Creating new NIOServer on {}", bindTo);
		channel = ServerSocketChannel.open();
		channel.bind(bindTo);

		this.listener = listener == null ? NIOServerListener.NOP : listener;
		this.workers = workers;
	}

	/**
	 * wait and accept new connection
	 */
	@Override
	public void run() {
		listener.starting(this);
		Thread t = Thread.currentThread();
		try {
			workers.start();
			while (!t.isInterrupted()) {
				register(channel.accept());
			}
			listener.closing(this, null);
		} catch (IOException e) {
			listener.closing(this, e);
			log.error("error on {}", this, e);
		}
	}

	/**
	 * register a client socket to one ioworker
	 * 
	 * @param socket socket to register
	 * @return the handler
	 * @throws IOException in case of error
	 */
	public Handler register(SocketChannel socket) throws IOException {
		return workers.register(socket);
	}

	/**
	 * return the address this server listen to
	 * 
	 * @return the address this server is bound to
	 */
	public InetSocketAddress getLocalAddress() {
		try {
			return (InetSocketAddress) channel.getLocalAddress();
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("NIOServer[");
		if (channel.isOpen()) {
			try {
				sb.append(channel.getLocalAddress());
			} catch (IOException e) {
				sb.append("failed to get local address");
			}
		} else
			sb.append("closed");
		return sb.append(']').toString();
	}
}
