/**
 * 
 */
package unknow.server.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.ServletManager;
import unknow.server.nio.NIOConnection;
import unknow.server.util.io.BuffersUtils;
import unknow.server.util.pool.Pool;

public class HttpConnection extends NIOConnection {
	private static final Logger logger = LoggerFactory.getLogger(HttpConnection.class);

	private final ExecutorService executor;
	private final ServletContextImpl ctx;
	private final int keepAliveIdle;

	private Future<?> f = CompletableFuture.completedFuture(null);
	private HttpProcessor p;

	/**
	 * create new RequestBuilder
	 * @param pool 
	 */
	public HttpConnection(Pool<NIOConnection> pool, ExecutorService executor, ServletContextImpl ctx, int keepAliveIdle) {
		super(pool);
		this.executor = executor;
		this.keepAliveIdle = keepAliveIdle;
		this.ctx = ctx;
	}

	@Override
	public final void onRead() throws InterruptedException {
		if (!f.isDone())
			return;
		if (!p.init(this))
			return;
//		int i = BuffersUtils.indexOf(pendingRead, CRLF2, 0, MAX_START_SIZE);
//		if (i == -1)
//			return;
//		if (i == -2) {
//			error(HttpError.BAD_REQUEST);
//			return;
//		}
		f = executor.submit(p);
	}

	@Override
	public final void onWrite() { // OK
	}

	private final void error(HttpError e) {
		try {
			OutputStream out = getOut();
			out.write(e.empty());
			out.close();
		} catch (@SuppressWarnings("unused") IOException ex) { // OK
		}
	}

	private void cleanup() {
		f.cancel(true);
		p.close();
		p = new HttpProcessor11(ctx, keepAliveIdle);
		pendingRead.clear();
	}

	@Override
	public boolean closed(long now, boolean stop) {
		if (stop)
			return f.isDone();

		if (isClosed())
			return true;
		if (!f.isDone())
			return false;

		if (keepAliveIdle > 0) {
			long e = now - keepAliveIdle;
			if (lastRead() <= e && lastWrite() <= e)
				return true;
		}

		// TODO check request timeout
		return false;
	}

	@Override
	protected final void onFree() {
		f.cancel(true);
		cleanup();
	}
}