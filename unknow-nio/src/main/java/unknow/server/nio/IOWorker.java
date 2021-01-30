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

/**
 * Thread responsible of all io
 * 
 * @author unknow
 */
public class IOWorker extends Thread {
	/** used for all synchronization */
	private final Object mutex = new Object();
	/** this worker id */
	private final int id;
	/** factory of handlers */
	private final HandlerFactory handlers;
	/** the selector */
	private final Selector selector;
	/** the listener */
	private final NIOServerListener listener;

	/** local cache to read/write */
	private final ByteBuffer buf = ByteBuffer.allocateDirect(4096);

	/**
	 * create new IOWorker
	 * 
	 * @param id       the worker id
	 * @param handlers handlers factory
	 * @param listener listener to use
	 * @throws IOException
	 */
	public IOWorker(int id, HandlerFactory handlers, NIOServerListener listener) throws IOException {
		super("IOWorker-" + id);
		setDaemon(true);
		this.id = id;
		this.handlers = handlers;
		this.listener = listener;
		this.selector = Selector.open();
	}

	/**
	 * register a new socket to this thread
	 * 
	 * @param socket the socket to register
	 * @throws IOException
	 */
	public Handler register(SocketChannel socket) throws IOException {
		synchronized (mutex) {
			selector.wakeup();
			socket.configureBlocking(false);
			Handler handler = handlers.get(socket.register(selector, SelectionKey.OP_READ));
			listener.accepted(id, handler);
			return handler;
		}
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			try {
				if (selector.select() > 0)
					processSelection();

				synchronized (mutex) {
					for (SelectionKey key : selector.keys()) {
						Handler h = (Handler) key.attachment();
						if (!key.isValid() || h.isClosed()) {
							handlers.free(h);
							key.cancel();
							key.channel().close();
							listener.closed(id, h);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
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

			// Tests whether this key's channel is ready to accept a new socket connection
			if (key.isValid() && key.isReadable()) {
				h.readFrom(channel, buf);
			}
			if (key.isValid() && key.isWritable()) {
				h.writeInto(channel, buf);
			}
		}
	}
}