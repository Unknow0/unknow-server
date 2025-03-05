package unknow.server.servlet.http11;

import java.util.Collection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletResponseImpl;
import unknow.server.servlet.utils.OrderedLock;

public class Http11ServletResponse extends ServletResponseImpl {
	protected final OrderedLock lock;
	private final int id;
	private final HttpResponse res;
	private boolean connectionClose;

	private Http11ServletOutput rawOutput;

	public Http11ServletResponse(ChannelHandlerContext out, ServletContextImpl ctx, Http11ServletRequest req, OrderedLock lock, int id, boolean connectionClose) {
		super(out, ctx, req);

		this.lock = lock;
		this.id = id;
		this.connectionClose = connectionClose;

		res = new DefaultHttpResponse(req.req().protocolVersion(), HttpResponseStatus.OK);
		rawOutput = new Http11ServletOutput(out, this);
	}

	public void setConnectionClose() {
		connectionClose = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Http11ServletOutput rawOutput() {
		return rawOutput;
	}

	@Override
	protected void doCommit() throws InterruptedException {
		lock.waitUntil(id);
		if (connectionClose)
			res.headers().set("connection", "close");
		if (!res.headers().contains("content-length") && !res.headers().contains("transfer-encoding")) {
			if (rawOutput.isClosed())
				res.headers().set("content-length", rawOutput.remaingSize());
			else
				res.headers().set("transfer-encoding", "chunked");
		}
		ctx.write(res);
	}

	@Override
	protected void doReset(boolean full) {
		res.setStatus(HttpResponseStatus.OK);
		if (full)
			res.headers().clear();
	}

	@Override
	public String getHeader(String name) {
		return res.headers().get(name);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		return res.headers().getAll(name);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return res.headers().names();
	}

	@Override
	public boolean containsHeader(String name) {
		return res.headers().contains(name);
	}

	@Override
	public void setHeader(String name, String value) {
		if (!isCommitted())
			res.headers().set(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		if (!isCommitted())
			res.headers().add(name, value);
	}

	@Override
	public void setStatus(int sc) {
		if (!isCommitted())
			res.setStatus(HttpResponseStatus.valueOf(sc));
	}

	@Override
	public int getStatus() {
		return res.status().code();
	}
}
