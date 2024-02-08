package unknow.server.http;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.http.servlet.ServletContextImpl;
import unknow.server.http.servlet.ServletRequestImpl;
import unknow.server.http.servlet.ServletResponseImpl;
import unknow.server.http.utils.EventManager;
import unknow.server.http.utils.ServletManager;
import unknow.server.nio.NIOConnection.Out;

public abstract class HttpProcessor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(HttpProcessor.class);

	protected final HttpConnection co;
	protected final ServletContextImpl ctx;
	protected final ServletManager servlets;
	protected final EventManager events;
	protected final int keepAliveIdle;

	protected HttpProcessor(HttpConnection co) {
		this.co = co;
		this.ctx = co.getCtx();
		this.servlets = ctx.getServletManager();
		this.events = ctx.getEvents();
		this.keepAliveIdle = co.getkeepAlive();
	}

	protected abstract boolean fillRequest(ServletRequestImpl req) throws InterruptedException, IOException;

	protected abstract void doRun(ServletRequestImpl req, ServletResponseImpl res) throws IOException;

	@SuppressWarnings("resource")
	@Override
	public final void run() {
		boolean close = false;
		ServletRequestImpl req = co.req;
		ServletResponseImpl res = co.res;

		Out out = co.getOut();
		try {
			if (!fillRequest(req))
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
			if (close)
				out.close();
			else
				out.flush();
		}
	}

	public static interface HttpProcessorFactory {
		/**
		 * create a processor if it can process it
		 * @param co the connection
		 * @return the processor or null
		 * @throws InterruptedException on interrupt
		 */
		HttpProcessor create(HttpConnection co) throws InterruptedException;
	}
}
