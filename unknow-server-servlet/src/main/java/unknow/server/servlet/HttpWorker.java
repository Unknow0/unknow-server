package unknow.server.servlet;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.UnavailableException;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.impl.ServletResponseImpl;

public class HttpWorker implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(HttpWorker.class);

	protected final ServletContextImpl ctx;
	protected final ServletRequestImpl req;
	protected final ServletResponseImpl res;

	public HttpWorker(ServletContextImpl ctx, ServletRequestImpl req, ServletResponseImpl res) {
		this.ctx = ctx;
		this.req = req;
		this.res = res;
	}

	@Override
	public void run() {
		try {
			ctx.events().fireRequestInitialized(req);
			FilterChain s = ctx.servlets().find(req);
			try {
				s.doFilter(req, res);
			} catch (UnavailableException e) {
				// TODO add page with retry-after
				res.sendError(503, e, null);
			} catch (Exception e) {
				logger.error("failed to service '{}'", s, e);
				if (!res.isCommitted())
					res.sendError(500, e, null);
			}
			ctx.events().fireRequestDestroyed(req);
		} catch (Throwable e) {
			logger.error("processor error", e);
			try {
				if (!res.isCommitted())
					res.sendError(500);
			} catch (@SuppressWarnings("unused") IOException e1) { //ok
			}
			if (e instanceof InterruptedException)
				Thread.currentThread().interrupt();
		} finally {
			req.release();
			try {
				res.close();
			} catch (@SuppressWarnings("unused") InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (@SuppressWarnings("unused") IOException e) { // OK
			}
		}
	}
}
