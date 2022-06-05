/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread responsible of all io
 * 
 * @author unknow
 */
public class IOWorker extends Thread {
	private static final Logger log = LoggerFactory.getLogger(IOWorker.class);

	/** used for all synchronization */
	private final Object mutex = new Object();
	/** this worker id */
	private final int id;
	/** the selector */
	private final Selector selector;
	/** the timeout on select method */
	private final long timeout;
	/** the listener */
	private final NIOServerListener listener;

	/** local cache to read/write */
	private final ByteBuffer buf = ByteBuffer.allocateDirect(4096);

	/**
	 * create new IOWorker
	 * 
	 * @param id       the worker id
	 * @param listener listener to use
	 * @param timeout  the timeout on select
	 * @throws IOException
	 */
	public IOWorker(int id, NIOServerListener listener, long timeout) throws IOException {
		super("IOWorker-" + id);
		this.id = id;
		this.listener = listener;
		this.selector = Selector.open();
		this.timeout = timeout;
	}

	/**
	 * register a new socket to this thread
	 * 
	 * @param socket  the socket to register
	 * @param handler the handler
	 * @throws IOException
	 */
	public void register(SocketChannel socket, Connection handler) throws IOException {
		synchronized (mutex) {
			socket.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			socket.configureBlocking(false);
			handler.attach(socket.register(selector, SelectionKey.OP_READ));
			selector.wakeup();
			listener.accepted(id, handler);
		}
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {
				select(timeout, false);
			} catch (IOException e) {
				log.error("failed to execute", e);
			} catch (InterruptedException e) {
				break;
			}
		}
		// stop mode
		// TODO add timeout ?
		while (!selector.keys().isEmpty()) {
			try {
				log.info("wait {} connection before close", selector.keys().size());
				select(500, true);
			} catch (Throwable e) {
				log.error("failed to execute", e);
			}
		}
		try {
			selector.close();
		} catch (Exception e) {
			log.error("failed to close selector", e);
		}
	}

	private void select(long timeout, boolean close) throws IOException, InterruptedException {
		if (selector.select(timeout) > 0)
			processSelection();

		synchronized (mutex) {
			long now = System.currentTimeMillis();
			for (SelectionKey key : selector.keys()) {
				Connection h = (Connection) key.attachment();
				if (!key.isValid() || h.closed(now, close)) {
					listener.closed(id, h);
					key.cancel();
					key.channel().close();
					h.free();
				}
			}
		}
	}

	/**
	 * the selection loop
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private final void processSelection() throws IOException, InterruptedException {
		Iterator<SelectionKey> it = selector.selectedKeys().iterator();

		while (it.hasNext()) {
			SelectionKey key = it.next();
			it.remove();
			Connection h = (Connection) key.attachment();
			SocketChannel channel = (SocketChannel) key.channel();

			if (key.isValid() && key.isWritable()) {
				try {
					h.writeInto(channel, buf);
				} catch (IOException e) {
					log.error("failed to write", h, e);
					channel.close();
				}
			}

			// Tests whether this key's channel is ready to accept a new socket connection
			if (key.isValid() && key.isReadable()) {
				try {
					h.readFrom(channel, buf);
				} catch (IOException e) {
					log.error("failed to read {}", h, e);
					channel.close();
				}
			}
		}
	}
}