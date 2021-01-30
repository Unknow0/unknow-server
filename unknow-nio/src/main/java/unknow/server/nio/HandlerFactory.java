/**
 * 
 */
package unknow.server.nio;

import java.nio.channels.SelectionKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * factory of handler
 * 
 * @author unknow
 */
public abstract class HandlerFactory {
	/** idle handlers cache */
	private final Queue<Handler> cache = new ConcurrentLinkedQueue<>();

	/**
	 * get or create a handler
	 * 
	 * @return a handler
	 */
	public final Handler get(SelectionKey key) {
		Handler h = cache.poll();
		if (h == null)
			h = create();

		key.attach(h);
		h.key = key;

		return h;
	}

	/**
	 * free a unused handler
	 * 
	 * @param h handler
	 */
	public final void free(Handler h) {
		h.reset();
		cache.offer(h);
	}

	/** create a new handler */
	protected abstract Handler create();
}
