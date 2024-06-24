/**
 * 
 */
package unknow.server.servlet;

import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContext;
import unknow.server.nio.NIOConnection;
import unknow.server.servlet.HttpProcessor.HttpProcessorFactory;
import unknow.server.servlet.http11.Http11Processor;
import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.utils.EventManager;
import unknow.server.servlet.utils.ServletManager;

public class HttpConnection extends NIOConnection {
	private static final Logger logger = LoggerFactory.getLogger(HttpConnection.class);

	private static final HttpProcessorFactory[] VERSIONS = new HttpProcessorFactory[] { Http2Processor.Factory, Http11Processor.Factory };

	private final ExecutorService executor;
	protected final ServletContextImpl ctx;
	protected final ServletManager manager;
	protected final EventManager events;
	private final int keepAliveIdle;

	private HttpProcessor p;

	/**
	 * create new RequestBuilder
	 * @param executor the executor
	 * @param ctx the servlet context
	 * @param events 
	 * @param manager 
	 */
	protected HttpConnection(SelectionKey key, ExecutorService executor, ServletContextImpl ctx, ServletManager manager, EventManager events, int keepAliveIdle) {
		super(key);
		this.executor = executor;
		this.ctx = ctx;
		this.manager = manager;
		this.events = events;
		this.keepAliveIdle = keepAliveIdle;
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
		if (stop)
			return p == null || p.isClosable(stop);

		if (isClosed())
			return true;

		if (p != null && !p.isClosable(stop))
			return false;

		if (p == null && lastRead() < now - 1000) {
			logger.warn("request timeout {}", this);
			return true;
		}

		if (pendingWrite.isEmpty() && keepAliveIdle > 0) {
			long e = now - keepAliveIdle;
			if (lastRead() <= e && lastWrite() <= e) {
				logger.info("keep alive idle reached {}", this);
				return true;
			}
		}

		return false;
	}

	@Override
	protected final void onFree() {
		if (p != null) {
			p.close();
			p = null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Future<T> submit(Runnable r) {
		return (Future<T>) executor.submit(r);
	}

	public ServletContext getCtx() {
		return ctx;
	}

	public int getkeepAlive() {
		return keepAliveIdle;
	}
}