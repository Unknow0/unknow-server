/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author unknow
 */
public class NIOLoop implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(NIOLoop.class);

	/** the name */
	protected final String name;
	/** the thread that will run the loop */
	private final Thread t;
	/** the selection timeout */
	protected final long timeout;
	/** the listener */
	protected final NIOServerListener listener;
	/** the selector */
	protected final Selector selector;

	private volatile boolean closing;

	/**
	 * create new loop
	 * @param name the thread name
	 * @param timeout selection timeout
	 * @param listener listener to use
	 * @throws IOException on ioexception
	 */
	public NIOLoop(String name, long timeout, NIOServerListener listener) throws IOException {
		this.name = name;
		this.t = new Thread(this, name);
		this.timeout = timeout;
		this.listener = listener;
		this.selector = Selector.open();
	}

	/**
	 * start the loop
	 */
	public final void start() {
		t.start();
	}

	/**
	 * stop the loop
	 */
	public final void stop() {
		closing = true;
		selector.wakeup();
	}

	/**
	 * bloc until the loop finished or we are interrupted
	 */
	public final void await() {
		try {
			t.join();
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public final void run() {
		onStartup();
		while (!closing) {
			try {
				select(timeout, false);
			} catch (IOException e) {
				logger.error("failed to execute", e);
			} catch (InterruptedException e) {
				logger.info("Interrupted", e);
				break;
			}
		}
		beforeStop();
		if (!selector.isOpen()) {
			afterStop();
			return;
		}

		// stop mode
		// TODO add timeout ?
		while (!selector.keys().isEmpty()) {
			try {
				logger.info("wait {} connection before close", selector.keys().size());
				select(500, true);
			} catch (Exception e) {
				logger.error("failed to execute", e);
			}
		}
		try {
			selector.close();
		} catch (Exception e) {
			logger.error("failed to close selector", e);
		}
		afterStop();
	}

	private final void select(long timeout, boolean close) throws IOException, InterruptedException {

		int l = selector.select(timeout);
		long now = System.nanoTime();
		if (l > 0) {
			Iterator<SelectionKey> it = selector.selectedKeys().iterator();
			while (it.hasNext()) {
				SelectionKey next = it.next();
				it.remove();
				try {
					selected(now, next);
				} catch (IOException e) {
					logger.warn("{}", next, e);
					next.cancel();
				}
			}
		}
		onSelect(now, close);
		listener.onSelect(name, now);
	}

	/**
	 * called on server startup
	 */
	protected void onStartup() { // for override
	}

	/**
	 * call for each selected key
	 * @param now nanoTime
	 * @param key the selected key
	 * 
	 * @throws IOException on ioexception
	 * @throws InterruptedException on interrupt
	 */
	@SuppressWarnings("unused")
	protected void selected(long now, SelectionKey key) throws IOException, InterruptedException { // for override
	}

	/**
	 * process after each selection loop
	 * @param now nanoTime
	 * @param close true if we are closing
	 * 
	 * @throws InterruptedException on interrupt
	 */
	@SuppressWarnings("unused")
	protected void onSelect(long now, boolean close) throws InterruptedException { // for override
	}

	/**
	 * called before the server enter the stop mode
	 */
	protected void beforeStop() { // for override
	}

	/**
	 * called when the server stopped
	 */
	protected void afterStop() { // for override
	}
}
