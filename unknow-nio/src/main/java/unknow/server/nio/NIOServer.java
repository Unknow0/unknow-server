/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedByInterruptException;
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
	/** the address we will bind to */
	private final SocketAddress bindTo;

	/** the listener */
	private final NIOServerListener listener;
	/** the channel from which we accept connection */
	private final ServerSocketChannel channel;
	/** the workers to handle the connection */
	private final NIOWorkers workers;

	/** the HandlerFactory */
	private final HandlerFactory factory;

	private Thread t;

	/**
	 * create new Server
	 * 
	 * @param bindTo   address to bind to
	 * @param workers  workers handler
	 * @param listener listener
	 * 
	 * @throws IOException
	 */
	public NIOServer(SocketAddress bindTo, NIOWorkers workers, HandlerFactory factory, NIOServerListener listener) throws IOException {
		log.debug("Creating new NIOServer on {}", bindTo);
		this.bindTo = bindTo;
		this.workers = workers;
		this.factory = factory;
		this.listener = listener == null ? NIOServerListener.NOP : listener;
		this.channel = ServerSocketChannel.open();

	}

	/**
	 * wait and accept new connection
	 */
	@Override
	public void run() {
		if (t != null)
			throw new IllegalStateException("server already running");
		t = Thread.currentThread();
		listener.starting(this);
		try {
			workers.start();
			channel.bind(bindTo);
			while (!Thread.interrupted())
				register(channel.accept());
			listener.closing(this, null);
		} catch (ClosedByInterruptException e) {
			Thread.interrupted(); // clean interrupted state for join
			listener.closing(this, null);
		} catch (IOException e) {
			listener.closing(this, e);
			log.error("error on {}", this, e);
		}
		try {
			channel.close();
			workers.stop();
		} catch (Exception e) { // OK
			log.warn("error on closing {}", this, e);
		}
		log.info("done");
	}

	/**
	 * Gracefully stop the server
	 * @throws InterruptedException 
	 */
	public void stop() throws InterruptedException {
		t.interrupt();
		t.join();
	}

	/**
	 * register a client socket to one ioworker
	 * 
	 * @param socket socket to register
	 * @throws IOException in case of error
	 */
	public void register(SocketChannel socket) throws IOException {
		workers.register(socket, factory.get());
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
		if (channel.isOpen())
			sb.append(bindTo);
		else
			sb.append("closed");
		return sb.append(']').toString();
	}
}
