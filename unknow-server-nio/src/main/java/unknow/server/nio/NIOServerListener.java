/**
 * 
 */
package unknow.server.nio;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author unknow
 */
public interface NIOServerListener {

	/**
	 * called when the server start
	 * 
	 * @param server the server starting
	 */
	void starting(NIOServer server);

	/**
	 * a new connection is created
	 * 
	 * @param id worker id
	 * @param h  client handler
	 */
	void accepted(int id, NIOConnectionAbstract h);

	/**
	 * a connection is closed
	 * 
	 * @param id worker id
	 * @param h  client handler
	 */
	void closed(int id, NIOConnectionAbstract h);

	/**
	 * call before the server stop
	 * 
	 * @param server the server closing
	 * @param e      the exception or null if the server is closed normally
	 */
	void closing(NIOServer server, Exception e);

	/**
	 * do nothing
	 */
	public static final NIOServerListener NOP = new NIOServerListener() {
		@Override
		public void starting(NIOServer server) { // OK
		}

		@Override
		public void accepted(int id, NIOConnectionAbstract h) { // OK
		}

		@Override
		public void closed(int id, NIOConnectionAbstract h) { // OK
		}

		@Override
		public void closing(NIOServer server, Exception e) { // OK
		}

		@Override
		public String toString() {
			return "NIOServerListener.NOP";
		}
	};

	/**
	 * Listener that log all event
	 */
	public static final NIOServerListener LOG = new NIOServerListener() {
		private final Logger logger = LoggerFactory.getLogger(NIOServerListener.class);

		private final AtomicInteger c = new AtomicInteger();

		@Override
		public void starting(NIOServer server) {
			logger.info("starting {}", server);
		}

		@Override
		public void accepted(int id, NIOConnectionAbstract h) {
			logger.info("Worker-{} accepted {} ({})", id, h, c.incrementAndGet());
		}

		@Override
		public void closed(int id, NIOConnectionAbstract h) {
			logger.info("Worker-{} closed {} ({})", id, h, c.decrementAndGet());
		}

		@Override
		public void closing(NIOServer server, Exception e) {
			logger.info("closing {}", server, e);
		}

		@Override
		public String toString() {
			return "NIOServerListener.LOG";
		}
	};
}
