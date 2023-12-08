package unknow.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.DispatcherType;
import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.servlet.ServletResponseImpl;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.ServletManager;
import unknow.server.nio.NIOConnection.Out;
import unknow.server.util.io.Buffers;

public abstract class HttpProcessor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(HttpProcessor.class);

	private HttpConnection co;
	protected final ServletContextImpl ctx;
	protected final ServletManager servlets;
	protected final EventManager events;
	protected final int keepAliveIdle;

	protected ServletResponseImpl res;
	protected ServletRequestImpl req;

	public HttpProcessor(ServletContextImpl ctx, int keepAliveIdle) {
		this.ctx = ctx;
		this.servlets = ctx.getServletManager();
		this.events = ctx.getEvents();
		this.keepAliveIdle = keepAliveIdle;
	}

	protected abstract boolean canProcess(HttpConnection co) throws InterruptedException;

	protected abstract boolean fillRequest() throws InterruptedException, IOException;

	protected abstract void doRun(ServletRequestImpl req, ServletResponseImpl res) throws IOException;

	public final boolean init(HttpConnection co) throws InterruptedException {
		if (!canProcess(co))
			return false;
		this.co = co;
		return true;
	}

	@SuppressWarnings("resource")
	@Override
	public void run() {
		boolean close = false;

		Out out = co.getOut();
		try {
			res = new ServletResponseImpl(ctx, this);
			req = new ServletRequestImpl(ctx, this, DispatcherType.REQUEST);

			if (!fillRequest())
				return;

			if ("100-continue".equals(req.getHeader("expect"))) {
				out.write(HttpError.CONTINUE.encoded);
				out.write('\r');
				out.write('\n');
				out.flush();
			}

			close = keepAliveIdle == 0 || !"keep-alive".equals(req.getHeader("connection"));
			if (!close)
				res.setHeader("connection", "keep-alive");
			events.fireRequestInitialized(req);
			doRun(req, res);
			events.fireRequestDestroyed(req);
			res.close();
		} catch (@SuppressWarnings("unused") ProcessDoneException e) { //ok
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			logger.error("processor error", e);
			try {
				res.sendError(500);
			} catch (@SuppressWarnings("unused") IOException e1) { //ok
			}
		} finally {
			co = null;
			if (close)
				out.close();
			else
				out.flush();
		}
	}

	public final void close() {
		co = null;
	}

	protected Buffers readBuffer() {
		if (co == null)
			throw new ProcessDoneException();
		return co.pendingRead;
	}

	public OutputStream getOut() {
		if (co == null)
			throw new ProcessDoneException();
		return co.getOut();
	}

	public InputStream getIn() {
		if (co == null)
			throw new ProcessDoneException();
		return co.getIn();
	}
}
