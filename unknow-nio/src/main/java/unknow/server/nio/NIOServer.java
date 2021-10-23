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
	private final IOWorker[] workers;

	private int i = 0;
	private final int len;

	/**
	 * create new Server
	 * 
	 * @param bindTo     address to bind to
	 * @param iothread   number of io thread
	 * @param factory    handler factory
	 * @param listener   listener
	 * @param selectTime max wait time on Selector.select
	 * 
	 * @throws IOException
	 */
	public NIOServer(SocketAddress bindTo, int iothread, HandlerFactory factory, NIOServerListener listener, long selectTime) throws IOException {
		log.debug("Creating new NIOServer on {}, iothread={} factory={} listener={}", bindTo, iothread, factory, listener);
		channel = ServerSocketChannel.open();

		channel.bind(bindTo);

		if (listener == null)
			listener = NIOServerListener.NOP;

		len = iothread;
		workers = new IOWorker[len];
		for (int i = 0; i < len; i++)
			workers[i] = new IOWorker(i, factory, listener, selectTime);
		this.listener = listener;
	}

	/**
	 * wait and accept new connection
	 */
	@Override
	public void run() {
		listener.starting(this);
		Thread t = Thread.currentThread();
		try {
			for (int i = 0; i < workers.length; i++)
				workers[i].start();
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
	public synchronized Handler register(SocketChannel socket) throws IOException {
		Handler register = workers[i++].register(socket);
		if (i == len)
			i = 0;
		return register;
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
