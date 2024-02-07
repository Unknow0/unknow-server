/**
 * 
 */
package unknow.server.http;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.servlet.ServletResponseImpl;
import unknow.server.http.utils.ServletManager;
import unknow.server.nio.NIOConnection;

public class HttpConnection extends NIOConnection {
	private static final Logger logger = LoggerFactory.getLogger(HttpConnection.class);

	private final ExecutorService executor;
	private final ServletContextImpl ctx;
	private final int keepAliveIdle;

	private Future<?> exec = CompletableFuture.completedFuture(null);
	private HttpProcessor p;

	protected final ServletResponseImpl res;
	protected final ServletRequestImpl req;

	/**
	 * create new RequestBuilder
	 * @param executor the executor
	 * @param ctx the servlet context
	 */
	protected HttpConnection(ExecutorService executor, ServletContextImpl ctx, int keepAliveIdle) {
		this.executor = executor;
		this.keepAliveIdle = keepAliveIdle;
		this.ctx = ctx;
		this.res = new ServletResponseImpl(this);
		this.req = new ServletRequestImpl(this, DispatcherType.REQUEST);
	}

	@Override
	protected void onInit() {
		p = new HttpProcessor11(getCtx(), keepAliveIdle);
	}

	@Override
	public final void onRead() throws InterruptedException {
		if (!exec.isDone())
			return;
		if (!p.init(this))
			return;
		exec = executor.submit(p);
	}

	@Override
	public final void onWrite() { // OK
	}

	private void cleanup() {
		exec.cancel(true);
		p.close();
		p = new HttpProcessor11(getCtx(), keepAliveIdle);
		pendingRead.clear();
	}

	@Override
	public boolean closed(long now, boolean stop) {
		if (stop)
			return exec.isDone();

		if (isClosed())
			return true;
		if (!exec.isDone())
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
		exec.cancel(true);
		cleanup();
	}

	public void sendError(int sc, Throwable t, String msg) throws IOException {
		res.checkCommited();
		res.setStatus(sc);
		ServletManager manager = getCtx().getServletManager();
		FilterChain f = manager.getError(sc, t);
		if (f != null) {
			ServletRequestImpl r = new ServletRequestImpl(this, DispatcherType.ERROR);
			r.setMethod("GET");
			r.setAttribute("javax.servlet.error.status_code", sc);
			if (t != null) {
				r.setAttribute("javax.servlet.error.exception_type", t.getClass());
				r.setAttribute("javax.servlet.error.message", t.getMessage());
				r.setAttribute("javax.servlet.error.exception", t);
			}
			r.setAttribute("javax.servlet.error.request_uri", r.getRequestURI());
			r.setAttribute("javax.servlet.error.servlet_name", "");
			res.reset();
			try {
				f.doFilter(r, res);
				return;
			} catch (ServletException e) {
				logger.error("failed to send error", e);
			}
		}
		res.sendError(HttpError.fromStatus(sc), sc, msg);
	}

	public ServletContextImpl getCtx() {
		return ctx;
	}

}