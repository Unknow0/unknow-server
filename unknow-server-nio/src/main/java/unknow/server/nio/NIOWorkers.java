/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.function.Supplier;

/**
 * @author unknow
 */
public interface NIOWorkers {
	/**
	 * register a socket to an IOWorker
	 * 
	 * @param socket  the socket to register
	 * @param pool the connection factory
	 * @throws IOException on ioexception
	 * @throws InterruptedException  on interrupt
	 */
	void register(SocketChannel socket, Supplier<NIOConnection> pool) throws IOException, InterruptedException;

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
		public synchronized void register(SocketChannel socket, Supplier<NIOConnection> pool) throws IOException, InterruptedException {
			w[o++].register(socket, pool);
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
