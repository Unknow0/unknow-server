package unknow.server.servlet.http11;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Enumeration;

import io.netty.handler.codec.http.HttpRequest;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletRequestImpl;

public class Http11ServletRequest extends ServletRequestImpl {

	private final HttpRequest req;

	public Http11ServletRequest(ServletContextImpl ctx, String scheme, HttpRequest req, InetSocketAddress remote, InetSocketAddress local) {
		super(ctx, scheme, req.method().name(), req.uri(), req.protocolVersion().text(), remote, local);
		this.req = req;
	}

	public HttpRequest req() {
		return req;
	}

	@Override
	public String getHeader(String name) {
		return req.headers().get(name);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		return Collections.enumeration(req.headers().getAll(name));
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(req.headers().names());
	}

	@Override
	public long getDateHeader(String name) {
		return req.headers().getTimeMillis(name, -1);
	}

	@Override
	public int getIntHeader(String name) {
		return req.headers().getInt(name, -1);
	}

	@Override
	public long getContentLengthLong() {
		String s = req.headers().get("content-length");
		return s == null ? -1 : Long.parseLong(s);
	}

}
