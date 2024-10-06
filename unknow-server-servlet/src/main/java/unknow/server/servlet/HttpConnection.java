/**
 * 
 */
package unknow.server.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOConnectionAbstract;
import unknow.server.nio.NIOConnectionAbstract.Out;
import unknow.server.nio.NIOConnectionHandler;
import unknow.server.servlet.HttpProcessor.HttpProcessorFactory;
import unknow.server.servlet.http11.Http11Processor;
import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.utils.EventManager;
import unknow.server.servlet.utils.ServletManager;
import unknow.server.util.io.Buffers;

public final class HttpConnection implements NIOConnectionHandler {
	private static final Logger logger = LoggerFactory.getLogger(HttpConnection.class);

	private static final HttpProcessorFactory[] VERSIONS = new HttpProcessorFactory[] { Http2Processor.Factory, Http11Processor.Factory };

	private final ExecutorService executor;
	private final ServletContextImpl ctx;
	private final ServletManager manager;
	private final EventManager events;
	private final int keepAliveIdle;

	private NIOConnectionAbstract co;
	private HttpProcessor p;

	/**
	 * create new RequestBuilder
	 * @param executor the executor
	 * @param ctx the servlet context
	 * @param events 
	 * @param manager 
	 */
	protected HttpConnection(ExecutorService executor, ServletContextImpl ctx, ServletManager manager, EventManager events, int keepAliveIdle) {
		this.executor = executor;
		this.ctx = ctx;
		this.manager = manager;
		this.events = events;
		this.keepAliveIdle = keepAliveIdle;
	}

	@Override
	public void onInit(NIOConnectionAbstract co, SSLEngine sslEngine) {
		this.co = co;
		if (sslEngine != null) {
			sslEngine.setUseClientMode(false);

			SSLParameters params = new SSLParameters();
			params.setApplicationProtocols(new String[] { "h2", "http/1.1" });
			sslEngine.setSSLParameters(params);
		}
	}

	@Override
	public void onHandshakeDone(SSLEngine sslEngine) throws InterruptedException {
		if ("h2".equals(sslEngine.getApplicationProtocol()))
			p = new Http2Processor(this);
		else
			p = new Http11Processor(this);
		p.process();
	}

	@Override
	public final void onRead(Buffers b) throws InterruptedException {
		for (int i = 0; p == null && i < VERSIONS.length; i++)
			p = VERSIONS[i].create(this);

		if (p != null)
			p.process();
	}

	@Override
	public boolean closed(long now, boolean stop) {
		if (stop)
			return p == null || p.isClosable(stop);

		if (co.isClosed())
			return true;

		if (p != null && !p.isClosable(stop))
			return false;

		if (p == null && co.lastRead() < now - 1000) {
			logger.warn("request timeout {}", this);
			return true;
		}

		if (co.pendingWrite().isEmpty()) {
			if (co.getIn().isClosed())
				return true;
			if (keepAliveIdle > 0) {
				long e = now - keepAliveIdle;
				if (co.lastRead() <= e && co.lastWrite() <= e) {
					logger.info("keep alive idle reached {}", this);
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void onWrite() throws InterruptedException, IOException { // ok
	}

	@Override
	public final void onFree() {
		if (p != null) {
			p.close();
			p = null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Future<T> submit(Runnable r) {
		return (Future<T>) executor.submit(r);
	}

	public ServletContextImpl getCtx() {
		return ctx;
	}

	public int getkeepAlive() {
		return keepAliveIdle;
	}

	public ServletManager getServlet() {
		return manager;
	}

	public EventManager getEvents() {
		return events;
	}

	public InetSocketAddress getRemote() {
		return co.getRemote();
	}

	public InetSocketAddress getLocal() {
		return co.getLocal();
	}

	public Out getOut() {
		return co.getOut();
	}

	public InputStream getIn() {
		return co.getIn();
	}

	public void flush() {
		co.flush();
	}

	public Buffers pendingWrite() {
		return co.pendingWrite();
	}

	public Buffers pendingRead() {
		return co.pendingRead();
	}
}