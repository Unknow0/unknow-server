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
	 * @param socket  the socket to register
	 * @param handler the handler
	 * @throws IOException
	 */
	void register(SocketChannel socket, Connection handler) throws IOException;

	/**
	 * start the IOWorker
	 */
	void start();

	/**
	 * gracefully stop the workers
	 */
	void stop();

	/**
	 * wait for the worker to stop
	 */
	void await();

	/**
	 * socket will register between workers in round robin
	 * 
	 * @author unknow
	 */
	public static class RoundRobin implements NIOWorkers {
		private final NIOWorker[] w;
		private int i;

		public RoundRobin(NIOWorker[] workers) {
			this.w = workers;
			this.i = 0;
		}

		@Override
		public synchronized void register(SocketChannel socket, Connection handler) throws IOException {
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
				w[i].stop();
		}

		@Override
		public void await() {
			for (int i = 0; i < w.length; i++)
				w[i].await();
		}
	}
}
