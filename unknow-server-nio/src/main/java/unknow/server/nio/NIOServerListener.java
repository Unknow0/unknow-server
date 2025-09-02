/**
 * 
 */
package unknow.server.nio;

import java.util.Arrays;
import java.util.Collection;
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
	 * @param name worker name
	 * @param h  client handler
	 */
	void accepted(String name, NIOConnection h);

	/**
	 * a connection is closed
	 * 
	 * @param name worker name
	 * @param h  client handler
	 */
	void closed(String name, NIOConnection h);

	/**
	 * call before the server stop
	 * 
	 * @param server the server closing
	 * @param e      the exception or null if the server is closed normally
	 */
	void closing(NIOServer server, Exception e);

	/**
	 * call after selection done
	 * @param name the worker name
	 * @param now start of selection
	 */
	void onSelect(String name, long now);

	/**
	 * do nothing
	 */
	public static final NIOServerListener NOP = new NIOServerListener() {
		@Override
		public void starting(NIOServer server) { // OK
		}

		@Override
		public void accepted(String name, NIOConnection h) { // OK
		}

		@Override
		public void closed(String name, NIOConnection h) { // OK
		}

		@Override
		public void closing(NIOServer server, Exception e) { // OK
		}

		@Override
		public void onSelect(String name, long now) { // ok
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
		public void accepted(String name, NIOConnection h) {
			logger.info("{} accepted {} ({})", name, h, c.incrementAndGet());
		}

		@Override
		public void closed(String name, NIOConnection h) {
			logger.info("{} closed {} ({})", name, h, c.decrementAndGet());
		}

		@Override
		public void closing(NIOServer server, Exception e) {
			logger.info("closing {}", server, e);
		}

		@Override
		public void onSelect(String name, long now) { // ok
			logger.trace("{} selection duration: {}", name, System.nanoTime() - now);
		}

		@Override
		public String toString() {
			return "NIOServerListener.LOG";
		}
	};

	/**
	 * compose multiple NIOServerListener
	 */
	public static class Composite implements NIOServerListener {
		private static final NIOServerListener[] EMPTY = new NIOServerListener[0];
		private final NIOServerListener[] listeners;

		/**
		 * create new Handler composite
		 * 
		 * @param listeners list of listeners
		 */
		public Composite(NIOServerListener... listeners) {
			this.listeners = listeners;
		}

		/**
		 * create new Handlermposite
		 * 
		 * @param listeners list of listeners
		 */
		public Composite(Collection<NIOServerListener> listeners) {
			this.listeners = listeners.toArray(EMPTY);
		}

		@Override
		public void starting(NIOServer server) {
			for (int i = 0; i < listeners.length; i++)
				listeners[i].starting(server);
		}

		@Override
		public void accepted(String name, NIOConnection h) {
			for (int i = 0; i < listeners.length; i++)
				listeners[i].accepted(name, h);
		}

		@Override
		public void closed(String name, NIOConnection h) {
			for (int i = 0; i < listeners.length; i++)
				listeners[i].closed(name, h);
		}

		@Override
		public void closing(NIOServer server, Exception e) {
			for (int i = 0; i < listeners.length; i++)
				listeners[i].closing(server, e);
		}

		@Override
		public void onSelect(String name, long now) {
			for (int i = 0; i < listeners.length; i++)
				listeners[i].onSelect(name, now);
		}

		@Override
		public String toString() {
			return Arrays.toString(listeners);
		}
	}
}
