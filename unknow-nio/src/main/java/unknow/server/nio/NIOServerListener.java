/**
 * 
 */
package unknow.server.nio;

import java.util.Arrays;
import java.util.Collection;

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
	void accepted(int id, Handler h);

	/**
	 * a connection is closed
	 * 
	 * @param id worker id
	 * @param h  client handler
	 */
	void closed(int id, Handler h);

	/**
	 * call when the server stop
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
		public void starting(NIOServer server) {
		}

		@Override
		public void accepted(int id, Handler h) {
		}

		@Override
		public void closed(int id, Handler h) {
		}

		@Override
		public void closing(NIOServer server, Exception e) {
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
		private final Logger log = LoggerFactory.getLogger(NIOServerListener.class);

		@Override
		public void starting(NIOServer server) {
			log.info("starting {}", server);
		}

		@Override
		public void accepted(int id, Handler h) {
			log.info("Worker-{} accepted {}", id, h);
		}

		@Override
		public void closed(int id, Handler h) {
			log.info("Worker-{} closed {}", id, h);
		}

		@Override
		public void closing(NIOServer server, Exception e) {
			log.info("closing {}", server, e);
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
		 * create new Handlermposite
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
		public void accepted(int id, Handler h) {
			for (int i = 0; i < listeners.length; i++)
				listeners[i].accepted(id, h);
		}

		@Override
		public void closed(int id, Handler h) {
			for (int i = 0; i < listeners.length; i++)
				listeners[i].closed(id, h);
		}

		@Override
		public void closing(NIOServer server, Exception e) {
			for (int i = 0; i < listeners.length; i++)
				listeners[i].closing(server, e);
		}

		@Override
		public String toString() {
			return Arrays.toString(listeners);
		}
	}
}
