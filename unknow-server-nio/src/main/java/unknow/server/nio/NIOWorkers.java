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
	void register(SocketChannel socket, NIOConnection handler) throws IOException;

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
		private int o;

		public RoundRobin(NIOWorker[] workers) {
			this.w = workers;
			this.o = 0;
		}

		@Override
		public synchronized void register(SocketChannel socket, NIOConnection handler) throws IOException {
			w[o++].register(socket, handler);
			if (o == w.length)
				o = 0;
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
