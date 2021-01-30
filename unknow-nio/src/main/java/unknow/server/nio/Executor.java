/**
 * 
 */
package unknow.server.nio;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * static executor
 * 
 * @author unknow
 */
public class Executor {
	private final BlockingDeque<Runnable> tasks = new LinkedBlockingDeque<>();
	private final E[] threads;

	/**
	 * create new Executor
	 * 
	 * @param nbthread
	 * @param daemon
	 */
	public Executor(int nbthread, boolean daemon) {
		threads = new E[nbthread];
		for (int i = 0; i < nbthread; i++) {
			E e = new E();
			e.setName("Executor-" + i);
			e.setDaemon(daemon);
			e.start();
			threads[i] = e;
		}
	}

	/**
	 * submit a runnable to execute
	 * 
	 * @param r a runnable to run
	 */
	public void submit(Runnable r) {
		tasks.offer(r);
	}

	private final class E extends Thread {
		@Override
		public void run() {
			try {
				while (!isInterrupted()) {
					Runnable take = tasks.take();
					take.run();
				}
			} catch (InterruptedException e) { // OK
			}
		}
	}
}
