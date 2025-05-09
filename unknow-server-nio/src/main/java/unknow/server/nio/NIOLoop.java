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

	/** the thread that will run the loop */
	private final Thread t;
	/** the selection timeout */
	protected final long timeout;
	/** the selector */
	protected final Selector selector;

	/**
	 * create new loop
	 * @param name the thread name
	 * @param timeout selection timeout
	 * @throws IOException on ioexception
	 */
	public NIOLoop(String name, long timeout) throws IOException {
		this.t = new Thread(this, name);
		this.timeout = timeout;
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
		t.interrupt();
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
		while (!Thread.interrupted()) {
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
		onSelect(close);
		if (selector.select(timeout) == 0)
			return;
		Iterator<SelectionKey> it = selector.selectedKeys().iterator();

		while (it.hasNext()) {
			SelectionKey next = it.next();
			it.remove();
			try {
				selected(next);
			} catch (IOException e) {
				logger.warn("{}", next, e);
				next.cancel();
			}
		}
	}

	/**
	 * called on server startup
	 */
	protected void onStartup() { // for override
	}

	/**
	 * call for each selected key
	 * 
	 * @param key the selected key
	 * @throws IOException on ioexception
	 * @throws InterruptedException on interrupt
	 */
	@SuppressWarnings("unused")
	protected void selected(SelectionKey key) throws IOException, InterruptedException { // for override
	}

	/**
	 * process after each selection loop
	 * 
	 * @param close true if we are closing
	 * @throws InterruptedException on interrupt
	 */
	@SuppressWarnings("unused")
	protected void onSelect(boolean close) throws InterruptedException { // for override
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
