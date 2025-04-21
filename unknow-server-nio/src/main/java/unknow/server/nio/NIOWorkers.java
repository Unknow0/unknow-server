/**
 * 
 */
package unknow.server.nio;

import java.nio.channels.SocketChannel;

import unknow.server.nio.NIOServer.ConnectionFactory;

/**
 * @author unknow
 */
public interface NIOWorkers {
	/**
	 * register a socket to an IOWorker
	 * 
	 * @param socket  the socket to register
	 * @param factory the connection factory
	 */
	void register(SocketChannel socket, ConnectionFactory factory);

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

		/** @param workers the workers */
		public RoundRobin(NIOWorker[] workers) {
			this.w = workers;
			this.o = 0;
		}

		@Override
		public synchronized void register(SocketChannel socket, ConnectionFactory factory) {
			w[o++].register(socket, factory);
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
