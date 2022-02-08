/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
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
	public void register(SocketChannel socket, Handler handler) throws IOException {
		synchronized (mutex) {
			selector.wakeup();
			socket.configureBlocking(false);
			handler.attach(socket.register(selector, SelectionKey.OP_READ));
			listener.accepted(id, handler);
		}
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {
				if (selector.select(timeout) > 0)
					processSelection();

				synchronized (mutex) {
					for (SelectionKey key : selector.keys()) {
						Handler h = (Handler) key.attachment();
						if (!key.isValid() || h.isClosed()) {
							listener.closed(id, h);
							h.free();
							key.cancel();
							key.channel().close();
						}
					}
				}
			} catch (IOException e) {
				log.error("failed to execute", e);
			}
		}
		// stop mode
		// TODO add timeout ?
		while (!selector.keys().isEmpty()) {
			try {
				log.info("wait {} connection before close", selector.keys().size());
				if (selector.select(500) > 0)
					processSelection();
				synchronized (mutex) {
					for (SelectionKey key : selector.keys()) {
						Handler h = (Handler) key.attachment();
						if (!key.isValid() || h.isClosed() || h.isIdle()) {
							h.free();
							key.cancel();
							key.channel().close();
							listener.closed(id, h);
						}
					}
				}
			} catch (Throwable e) {
				log.error("failed to execute", e);
			}
		}
		log.info("{}", selector.keys().size());
		try {
			selector.close();
		} catch (Exception e) {
			log.error("failed to close selector", e);
		}
	}

	/**
	 * the selection loop
	 * 
	 * @throws IOException
	 */
	private final void processSelection() throws IOException {
		Iterator<SelectionKey> it = selector.selectedKeys().iterator();

		while (it.hasNext()) {
			SelectionKey key = it.next();
			it.remove();
			Handler h = (Handler) key.attachment();
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