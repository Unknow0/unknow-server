/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author unknow
 */
public interface NIOWorkers {
	/**
	 * register a socket to an IOWorker
	 * 
	 * @param socket the socket to register
	 * @param handler the handler
	 * @throws IOException
	 */
	void register(SocketChannel socket, Handler handler) throws IOException;

	/**
	 * start the IOWorker
	 */
	void start();

	/**
	 * gracefully stop the workers
	 */
	void stop();

	/**
	 * only one worker will be use
	 * 
	 * @author unknow
	 */
	public static final class Single implements NIOWorkers {
		private final IOWorker worker;

		public Single(IOWorker worker) {
			this.worker = worker;
		}

		@Override
		public void register(SocketChannel socket, Handler handler) throws IOException {
			worker.register(socket, handler);
		}

		@Override
		public void start() {
			worker.start();
		}

		@Override
		public void stop() {
			worker.interrupt();
			try {
				worker.join();
			} catch (InterruptedException e) { // OK
			}
		}
	}

	/**
	 * socket will register between workers in round robin
	 * 
	 * @author unknow
	 */
	public static class RoundRobin implements NIOWorkers {
		private static final Logger log = LoggerFactory.getLogger(RoundRobin.class);
		private final IOWorker[] w;
		private int i;

		public RoundRobin(IOWorker[] workers) {
			this.w = workers;
			this.i = 0;
		}

		@Override
		public synchronized void register(SocketChannel socket, Handler handler) throws IOException {
			w[i++].register(socket, handler);
			if (i == w.length)
				i = 0;
		}

		@Override
		public void start() {
			for (int i = 0; i < w.length; i++)
				w[i].start();
		}

		@Override
		public void stop() {
			for (int i = 0; i < w.length; i++)
				w[i].interrupt();
			try {
				for (int i = 0; i < w.length; i++)
					w[i].join();
			} catch (InterruptedException e) {
				log.info("", e);
			}
		}
	}
}
