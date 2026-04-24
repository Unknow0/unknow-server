package unknow.server.nio.worker;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collection;

import unknow.server.nio.NIOServer.ConnectionFactory;
import unknow.server.nio.NIOWorker;
import unknow.server.nio.NIOWorkers;

/**
 * socket will register between workers in round robin
 * 
 * @author unknow
 */
public abstract class AbstractNIOWorkers implements NIOWorkers {
	protected final NIOWorker[] w;

	/** @param workers the workers */
	protected AbstractNIOWorkers(NIOWorker[] workers) {
		this.w = workers;
	}

	protected abstract NIOWorker next();

	@Override
	public synchronized void register(SocketChannel socket, ConnectionFactory factory) throws IOException {
		next().register(socket, factory);
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

	@Override
	public Collection<NIOWorker> workers() {
		return Arrays.asList(w);
	}
}