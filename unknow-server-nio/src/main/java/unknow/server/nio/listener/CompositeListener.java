package unknow.server.nio.listener;

import java.util.Arrays;
import java.util.Collection;

import unknow.server.nio.NIOConnectionAbstract;
import unknow.server.nio.NIOServer;
import unknow.server.nio.NIOServerListener;

/**
 * compose multiple NIOServerListener
 */
public class CompositeListener implements NIOServerListener {
	private static final NIOServerListener[] EMPTY = new NIOServerListener[0];
	private final NIOServerListener[] listeners;

	/**
	 * create new Handler composite
	 * 
	 * @param listeners list of listeners
	 */
	public CompositeListener(NIOServerListener... listeners) {
		this.listeners = listeners;
	}

	/**
	 * create new CompositeListener
	 * 
	 * @param listeners list of listeners
	 */
	public CompositeListener(Collection<NIOServerListener> listeners) {
		this.listeners = listeners.toArray(EMPTY);
	}

	@Override
	public void starting(NIOServer server) {
		for (int i = 0; i < listeners.length; i++)
			listeners[i].starting(server);
	}

	@Override
	public void accepted(int id, NIOConnectionAbstract h) {
		for (int i = 0; i < listeners.length; i++)
			listeners[i].accepted(id, h);
	}

	@Override
	public void closed(int id, NIOConnectionAbstract h) {
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