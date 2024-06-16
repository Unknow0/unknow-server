package unknow.server.servlet.http2;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletInputStream;
import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.out.AbstractServletOutput;
import unknow.server.servlet.utils.PathUtils;

public class Http2Stream extends HttpWorker implements Http2FlowControl {
	private static final Logger logger = LoggerFactory.getLogger(Http2Stream.class);

	private final int id;
	private final Http2Processor p;
	final Http2ServletInput in;
	final Http2ServletOutput out;

	private int window;

	private volatile Future<?> exec;

	protected Http2Stream(HttpConnection co, int id, Http2Processor p, int window) {
		super(co);
		this.id = id;
		this.p = p;
		this.in = new Http2ServletInput();
		this.out = new Http2ServletOutput(res, p, id);

		this.window = window;

		this.exec = CompletableFuture.completedFuture(null);

		req.setProtocol("HTTP/2");
	}

	public final void addHeader(String name, String value) {
		logger.debug("addHeader {}: {}", name, value);
		if (name.charAt(0) != ':') {
			req.addHeader(name, value);
			return;
		}

		if (":method".equals(name))
			req.setMethod(value);
		else if (":path".equals(name))
			parsePath(value);
	}

	private void parsePath(String path) {
		int q = path.indexOf('?');
		if (q > 0) {
			req.setQuery(path.substring(q + 1));
			try (Reader r = new StringReader(req.getQueryString())) {
				PathUtils.pathQuery(r, req.getQueryParam());
			} catch (@SuppressWarnings("unused") IOException e) { // ok
			}
			path = path.substring(0, q);
		}
		req.setRequestUri(path);
	}

	public void start() {
		exec = co.submit(this);
	}

	@Override
	public void add(int v) {
		window += v;
		if (window < 0)
			window = Integer.MAX_VALUE;
	}

	@Override
	public ServletInputStream createInput() {
		return in;
	}

	@Override
	public AbstractServletOutput createOutput() {
		return out;
	}

	@Override
	public void commit() throws IOException {
		// write header

		try {
			p.sendHeaders(id, res);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

	@Override
	protected boolean doStart() throws IOException, InterruptedException {
		return true;
	}

	@Override
	protected void doDone() { // ok
	}

	public final void close(boolean stop) {
		in.close();
		if (stop)
			exec.cancel(true);
	}
}
