package unknow.server.servlet.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.DispatcherType;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.AbstractServletOutput;

public class Http2Stream extends HttpWorker implements Http2FlowControl {
	private static final Logger logger = LoggerFactory.getLogger(Http2Stream.class);

	private final int id;
	private final Http2Processor p;
	private final Http2ServletOutput out;

	private int flowWrite;
	private int flowRead;
	private final int flowThreshold;

	private volatile Future<?> exec;

	public Http2Stream(Http2Processor p, int id, int window) {
		super(p.co, new Http2Request(p.co, DispatcherType.REQUEST));
		this.id = id;
		this.p = p;
		this.out = new Http2ServletOutput(res, p, id);
		this.flowWrite = window;
		this.flowRead = 65535;
		this.flowThreshold = 65535;

		this.exec = CompletableFuture.completedFuture(null);
	}

	public int id() {
		return id;
	}

	public final void addHeader(String name, String value) {
		if (name.charAt(0) != ':') {
			req.addHeader(name, value);
			return;
		}

		if (":method".equals(name))
			req.setMethod(value);
		else if (":path".equals(name)) {
			logger.info("path {}", value);
			parsePath(value);
		}
	}

	private void parsePath(String path) {
		int q = path.indexOf('?');
		if (q > 0) {
			req.setQuery(path.substring(q + 1));
			path = path.substring(0, q);
		}
		req.setRequestUri(path);
	}

	public void start() {
		logger.debug("{}: Stream {} start", co, id);
		exec = co.submit(this);
	}

	@Override
	public int flowWrite() {
		return flowWrite;
	}

	@Override
	public void flowWrite(int v) {
		flowWrite -= v;
		p.flowWrite(v);
	}

	@Override
	public int flowRead() {
		return flowRead;
	}

	@Override
	public void flowRead(int v) throws IOException {
		if ((flowRead -= v) < flowThreshold)
			p.sendWindowUpdate(id, flowThreshold);
		p.flowRead(v);
	}

	@Override
	public AbstractServletOutput createOutput() {
		return out;
	}

	@Override
	public void commit() throws IOException {
		p.sendHeaders(id, res);
	}

	@Override
	protected boolean doStart() {
		return true;
	}

	@Override
	protected void doDone() { // ok
	}

	public final void close(boolean stop) {
		((Http2Request) req).close();
		if (stop)
			exec.cancel(true);
	}

	public boolean isClosed() {
		return exec.isDone();
	}

	public void append(ByteBuffer buf) {
		((Http2Request) req).append(buf);
	}
}
