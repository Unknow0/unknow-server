package unknow.server.nio.worker;

import java.util.concurrent.atomic.AtomicInteger;

import unknow.server.nio.NIOWorker;
import unknow.server.nio.NIOWorkers;

/**
 * socket will register between workers in round robin
 * 
 * @author unknow
 */

public final class RoundRobin extends AbstractNIOWorkers implements NIOWorkers {
	private final AtomicInteger o;

	/** @param workers the workers */
	private RoundRobin(NIOWorker[] workers) {
		super(workers);
		this.o = new AtomicInteger(0);
	}

	public static NIOWorkers create(NIOWorker[] w) {
		if ((w.length & (w.length - 1)) == 0)
			return new RoundRobinPow(w);
		return new RoundRobin(w);
	}

	@Override
	protected NIOWorker next() {
		return w[o.getAndIncrement() % w.length];
	}

	public static final class RoundRobinPow extends AbstractNIOWorkers implements NIOWorkers {
		private final AtomicInteger o;
		private final int m;

		/** @param workers the workers */
		private RoundRobinPow(NIOWorker[] workers) {
			super(workers);
			this.o = new AtomicInteger(0);
			this.m = workers.length - 1;
		}

		@Override
		protected NIOWorker next() {
			return w[o.getAndIncrement() & m];
		}
	}
}