/**
 * 
 */
package unknow.server.servlet;

import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOConnectionSSL;
import unknow.server.servlet.http11.Http11Processor;
import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.utils.EventManager;
import unknow.server.servlet.utils.ServletManager;

public class HttpConnectionSSL extends NIOConnectionSSL implements HttpConnection {
	private static final Logger logger = LoggerFactory.getLogger(HttpConnectionSSL.class);

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
	protected HttpConnectionSSL(SelectionKey key, SSLContext sslContext, ExecutorService executor, ServletContextImpl ctx, ServletManager manager, EventManager events,
			int keepAliveIdle) {
		super(key, sslContext);
		this.executor = executor;
		this.ctx = ctx;
		this.manager = manager;
		this.events = events;
		this.keepAliveIdle = keepAliveIdle;
	}

	@Override
	protected void onInit(SSLEngine sslEngine) {
		sslEngine.setUseClientMode(false);

		SSLParameters params = new SSLParameters();
		params.setApplicationProtocols(new String[] { "h2", "http/1.1" });
		sslEngine.setSSLParameters(params);
	}

	@Override
	protected void onHandshakeDone() throws InterruptedException {
		switch (sslEngine.getApplicationProtocol()) {
			case "h2":
				p = new Http2Processor(this);
				break;
			default:
				p = new Http11Processor(this);
		}
		p.process();
	}

	@Override
	public final void onRead() throws InterruptedException {
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

		if (pendingWrite().isEmpty() && keepAliveIdle > 0) {
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

	@Override
	@SuppressWarnings("unchecked")
	public <T> Future<T> submit(Runnable r) {
		return (Future<T>) executor.submit(r);
	}

	@Override
	public ServletContextImpl getCtx() {
		return ctx;
	}

	@Override
	public int getkeepAlive() {
		return keepAliveIdle;
	}

	@Override
	public ServletManager getServlet() {
		return manager;
	}

	@Override
	public EventManager getEvents() {
		return events;
	}
}