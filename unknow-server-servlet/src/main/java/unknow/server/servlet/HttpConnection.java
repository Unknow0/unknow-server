/**
 * 
 */
package unknow.server.servlet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Future;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.nio.NIOConnection;
import unknow.server.nio.NIOConnection.Out;
import unknow.server.nio.NIOConnectionHandler;
import unknow.server.nio.NIOWorker.WorkerTask;
import unknow.server.servlet.http11.Http11Processor;
import unknow.server.servlet.http2.Http2Processor;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.utils.EventManager;
import unknow.server.servlet.utils.ServletManager;

public final class HttpConnection implements NIOConnectionHandler {
	private static final Logger logger = LoggerFactory.getLogger(HttpConnection.class);

	private final ServletContextImpl ctx;
	private final ServletManager manager;
	private final EventManager events;
	private final int keepAliveMs;

	private NIOConnection co;
	private NIOConnectionHandler p;

	private long lastRead;
	private long lastWrite;

	/**
	 * create new RequestBuilder
	 * 
	 * @param ctx the servlet context
	 * @param manager servet manager
	 * @param events event manager
	 * @param keepAliveIdle max idle time in ms
	 */
	protected HttpConnection(ServletContextImpl ctx, ServletManager manager, EventManager events, int keepAliveIdle) {
		this.ctx = ctx;
		this.manager = manager;
		this.events = events;
		this.keepAliveMs = keepAliveIdle;
	}

	@Override
	public void onInit(NIOConnection co, long now, SSLEngine sslEngine) {
		this.co = co;
		this.lastRead = this.lastWrite = now;
		if (sslEngine != null) {
			sslEngine.setUseClientMode(false);

			SSLParameters params = new SSLParameters();
			params.setApplicationProtocols(new String[] { "h2", "http/1.1" });
			sslEngine.setSSLParameters(params);
		}
	}

	@Override
	public void onHandshakeDone(SSLEngine sslEngine, long now) throws IOException {
		if ("h2".equals(sslEngine.getApplicationProtocol()))
			p = new Http2Processor(this);
		else
			p = new Http11Processor(this);
	}

	@Override
	public final void onRead(ByteBuffer b, long now) throws IOException {
		lastRead = now;
		if (p == null) {
			if (Arrays.equals(b.array(), b.position(), b.limit(), Http2Processor.PRI, 0, Http2Processor.PRI.length))
				p = new Http2Processor(this);
			else
				p = new Http11Processor(this);
		}
		p.onRead(b, now);
	}

	@Override
	public void onWrite(long now) throws IOException {
		lastWrite = now;
		if (p != null)
			p.onWrite(now);
	}

	@Override
	public boolean canClose(long now, boolean stop) {
		if (stop)
			return p == null || p.canClose(now, stop);

		if (p == null) {
			if (lastRead < now - 1000) {
				logger.warn("request timeout {}", co);
				return true;
			}
			return false;
		}

		return p.canClose(now, stop);
	}

	@Override
	public void startClose() {
		if (p != null)
			p.startClose();
	}

	@Override
	public boolean finishClosing(long now) {
		return p == null || p.finishClosing(now);
	}

	@Override
	public final void doneClosing() {
		if (p != null) {
			p.doneClosing();
			p = null;
		}
	}

	public boolean keepAliveReached(long now) {
		if (keepAliveMs > 0) {
			long e = now - keepAliveMs - 500;
			if (lastRead <= e && lastWrite <= e) {
				logger.info("keep alive idle reached {}", co);
				return true;
			}
		}
		return false;
	}

	public final <T> Future<T> submit(Runnable r) {
		return co.submit(r);
	}

	public final void execute(WorkerTask task) {
		co.execute(task);
	}

	public void write(ByteBuffer buf) throws IOException {
		try {
			co.write(buf);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Failed to write", e);
		}
	}

	public boolean hasPendingWrites() {
		return co.hasPendingWrites();
	}

	public ServletContextImpl getCtx() {
		return ctx;
	}

	public int getkeepAlive() {
		return keepAliveMs;
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

	@SuppressWarnings("resource")
	public void flush() throws IOException {
		co.getOut().flush();
	}

	public Out getOut() {
		return co.getOut();
	}

	@Override
	public String toString() {
		return co.toString();
	}
}