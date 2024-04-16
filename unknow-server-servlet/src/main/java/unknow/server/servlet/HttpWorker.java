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
import unknow.server.servlet.utils.ServletManager;

public abstract class HttpWorker implements Runnable, HttpAdapter {
	private static final Logger logger = LoggerFactory.getLogger(HttpWorker.class);

	protected final HttpConnection co;
	protected final ServletManager manager;
	protected ServletRequestImpl req;
	protected ServletResponseImpl res;

	public HttpWorker(HttpConnection co) {
		this.co = co;
		this.manager = co.manager;
		this.req = new ServletRequestImpl(this, DispatcherType.REQUEST);
		this.res = new ServletResponseImpl(this);
	}

	@Override
	public final ServletContextImpl ctx() {
		return co.ctx;
	}

	@Override
	public final EventManager events() {
		return co.events;
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
			co.events.fireRequestInitialized(req);
			FilterChain s = manager.find(req);
			try {
				s.doFilter(req, res);
			} catch (UnavailableException e) {
				// TODO add page with retry-after
				sendError(503, e, null);
			} catch (Exception e) {
				logger.error("failed to service '{}'", s, e);
				if (!res.isCommitted())
					res.sendError(500);
			}
			co.events.fireRequestDestroyed(req);
			req.clearInput();
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
