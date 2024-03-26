/**
 * 
 */
package unknow.server.servlet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOConnection;
import unknow.server.servlet.HttpProcessor.HttpProcessorFactory;
import unknow.server.servlet.http11.Http11Processor;
import unknow.server.servlet.impl.ServletContextImpl;

public class HttpConnection extends NIOConnection {
	private static final Logger logger = LoggerFactory.getLogger(HttpConnection.class);

	private static final HttpProcessorFactory[] VERSIONS = new HttpProcessorFactory[] { /*Http2Processor.Factory, */Http11Processor.Factory };

	private final ExecutorService executor;
	private final ServletContextImpl ctx;
	private final int keepAliveIdle;

	private HttpProcessor p;

	/**
	 * create new RequestBuilder
	 * @param executor the executor
	 * @param ctx the servlet context
	 */
	protected HttpConnection(ExecutorService executor, ServletContextImpl ctx, int keepAliveIdle) {
		this.executor = executor;
		this.keepAliveIdle = keepAliveIdle;
		this.ctx = ctx;
	}

	@Override
	public final void onRead() throws InterruptedException {
		for (int i = 0; p == null && i < VERSIONS.length; i++)
			p = VERSIONS[i].create(this);

		if (p != null)
			p.process();
	}

	@Override
	public boolean closed(long now, boolean stop) {
		if (!pendingWrite.isEmpty())
			return false;

		if (stop)
			return p == null || p.isClosed();

		if (isClosed())
			return true;
		if (p != null && !p.isClosed())
			return false;

		if (pendingWrite.isEmpty() && keepAliveIdle > 0) {
			long e = now - keepAliveIdle;
			if (lastRead() <= e && lastWrite() <= e)
				return true;
		}

		// TODO check request timeout
		return false;
	}

	@Override
	protected final void onFree() {
		p.close();
		p = null;
		pendingRead.clear();
	}

	public Future<?> submit(Runnable r) {
		return executor.submit(r);
	}

	public ServletContextImpl getCtx() {
		return ctx;
	}

	public int getkeepAlive() {
		return keepAliveIdle;
	}
}