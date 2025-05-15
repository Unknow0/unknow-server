package unknow.server.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.UnavailableException;
import unknow.server.servlet.impl.AbstractServletOutput;
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

	protected HttpWorker(HttpConnection co, ServletRequestImpl req) {
		this.co = co;
		this.manager = co.getServlet();
		this.req = req;
		this.res = new ServletResponseImpl(this);
	}

	@Override
	public final ServletContextImpl ctx() {
		return co.getCtx();
	}

	@Override
	public final EventManager events() {
		return co.getEvents();
	}

	@Override
	public InetSocketAddress getRemote() {
		return co.getRemote();
	}

	@Override
	public InetSocketAddress getLocal() {
		return co.getLocal();
	}

	protected abstract boolean doStart() throws IOException, InterruptedException;

	protected abstract void doDone() throws IOException;

	@Override
	public void sendError(int sc, Throwable t, String msg) throws IOException {
		res.reset(false);
		FilterChain f = manager.getError(sc, t);
		if (f != null) {
			req.setAttribute("javax.servlet.error.status_code", sc);
			if (t != null) {
				req.setAttribute("javax.servlet.error.exception_type", t.getClass());
				req.setAttribute("javax.servlet.error.message", t.getMessage());
				req.setAttribute("javax.servlet.error.exception", t);
			}
			req.setAttribute("javax.servlet.error.request_uri", req.getRequestURI());
			req.setAttribute("javax.servlet.error.servlet_name", "");
			try {
				f.doFilter(req, res);
				return;
			} catch (ServletException e) {
				logger.error("failed to send error", e);
			}
		}
		res.setStatus(sc);
		if (msg == null) {
			HttpError e = HttpError.fromStatus(sc);
			msg = e == null ? "" : e.message;
		}
		try (PrintWriter w = res.getWriter()) {
			w.append("<html><body><p>Error ").append(Integer.toString(sc)).append(" ").append(msg.replace("<", "&lt;")).write("</p></body></html>");
		}
	}

	@Override
	public void run() {
		try {
			if (!doStart()) {
				logger.warn("init req failed");
				co.getOut().close();
				return;
			}
			doRun();
		} catch (Exception e) {
			logger.error("processor error", e);
			try {
				if (!res.isCommitted())
					res.sendError(500);
			} catch (@SuppressWarnings("unused") IOException e1) { //ok
			}
			if (e instanceof InterruptedException)
				Thread.currentThread().interrupt();
		} finally {
			try {
				doDone();
				co.flush();
			} catch (IOException e) {
				logger.warn("Failed to finish connection", e);
			}
		}
	}

	private final void doRun() throws IOException {
		co.getEvents().fireRequestInitialized(req);
		FilterChain s = manager.find(req);
		try {
			s.doFilter(req, res);
		} catch (UnavailableException e) {
			// TODO add page with retry-after
			sendError(503, e, null);
		} catch (Exception e) {
			logger.error("failed to service '{}'", s, e);
			if (!res.isCommitted())
				sendError(500, e, null);
		}
		co.getEvents().fireRequestDestroyed(req);
		req.clearInput();
		res.close();
	}

	@Override
	public ServletInputStream createInput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractServletOutput createOutput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void commit() throws IOException {
		// TODO Auto-generated method stub

	}
}
