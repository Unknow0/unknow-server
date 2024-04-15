package unknow.server.servlet;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.UnavailableException;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.impl.ServletResponseImpl;
import unknow.server.servlet.utils.EventManager;

public abstract class HttpWorker implements Runnable, HttpAdapter {
	private static final Logger logger = LoggerFactory.getLogger(HttpWorker.class);

	protected final HttpConnection co;
	protected final EventManager events;
	protected ServletRequestImpl req;
	protected ServletResponseImpl res;

	public HttpWorker(HttpConnection co) {
		this.co = co;
		this.events = co.getCtx().getEvents();
		this.req = new ServletRequestImpl(this, DispatcherType.REQUEST);
		this.res = new ServletResponseImpl(this);
	}

	@Override
	public ServletContextImpl ctx() {
		return co.getCtx();
	}

	protected abstract boolean doStart() throws IOException, InterruptedException;

	protected abstract void doDone();

	@Override
	public void run() {
		doRun();
	}

	protected final void doRun() {
		try {
			if (!doStart()) {
				logger.warn("init req failed");
				co.getOut().close();
				return;
			}
			events.fireRequestInitialized(req);
			FilterChain s = co.getCtx().getServletManager().find(req);
			try {
				s.doFilter(req, res);
				co.pendingRead.clear();
			} catch (UnavailableException e) {
				// TODO add page with retry-after
				res.sendError(503, e, null);
			} catch (Exception e) {
				logger.error("failed to service '{}'", s, e);
				if (!res.isCommitted())
					res.sendError(500);
			}
			events.fireRequestDestroyed(req);
			res.close();
		} catch (Exception e) {
			logger.error("processor error", e);
			try {
				if (!res.isCommitted())
					res.sendError(500);
			} catch (@SuppressWarnings("unused") IOException e1) { //ok
			}
		} finally {
			doDone();
			co.flush();
		}
	}
}
