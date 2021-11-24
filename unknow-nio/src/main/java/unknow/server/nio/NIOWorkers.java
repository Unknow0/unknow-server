/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author unknow
 */
public interface NIOWorkers {
	/**
	 * register a socket to an IOWorker
	 * 
	 * @param socket the socket to register
	 * @return the handler created for the socket
	 * @throws IOException
	 */
	Handler register(SocketChannel socket) throws IOException;

	/**
	 * start the IOWorker
	 */
	void start();

	/**
	 * stop the IOWorker
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
		public Handler register(SocketChannel socket) throws IOException {
			return worker.register(socket);
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
		private final IOWorker[] w;
		private int i;

		public RoundRobin(IOWorker[] workers) {
			this.w = workers;
			this.i = 0;
		}

		@Override
		public synchronized Handler register(SocketChannel socket) throws IOException {
			Handler register = w[i++].register(socket);
			if (i == w.length)
				i = 0;
			return register;
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
			} catch (InterruptedException e) { // OK
			}
		}
	}
}
