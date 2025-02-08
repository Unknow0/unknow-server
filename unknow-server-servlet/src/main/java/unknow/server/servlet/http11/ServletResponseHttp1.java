package unknow.server.servlet.http11;

import java.util.Collection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import unknow.server.servlet.http11.HandlerHttp.OrderedLock;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletResponseImpl;

public class ServletResponseHttp1 extends ServletResponseImpl {

	protected final OrderedLock lock;
	private final int id;
	private final HttpResponse res;

	private ServletOutputHttp1 rawOutput;

	public ServletResponseHttp1(ChannelHandlerContext out, ServletContextImpl ctx, ServletRequestHttp1 req, OrderedLock lock, int id) {
		super(out, ctx, req);

		this.lock = lock;
		this.id = id;

		res = new DefaultHttpResponse(req.req().protocolVersion(), HttpResponseStatus.OK);
		rawOutput = new ServletOutputHttp1(out, this);
	}

	@Override
	protected ServletOutputHttp1 rawOutput() {
		return rawOutput;
	}

	@Override
	protected void doCommit() throws InterruptedException {
		lock.waitUntil(id);
		if (!res.headers().contains("content-length") && !res.headers().contains("transfer-encoding"))
			res.headers().set("transfer-encoding", "chunked");
		System.out.println(res);
		out.write(res);
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
		res.headers().set(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		res.headers().add(name, value);
	}

	@Override
	public void setStatus(int sc) {
		res.setStatus(HttpResponseStatus.valueOf(sc));
	}

	@Override
	public int getStatus() {
		return res.status().code();
	}
}
