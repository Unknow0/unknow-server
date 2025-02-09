package unknow.server.servlet.http2;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

import io.netty.handler.codec.http2.Http2Headers;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletRequestImpl;

public class Http2ServletRequest extends ServletRequestImpl {

	private final Http2Headers headers;

	public Http2ServletRequest(ServletContextImpl ctx, Http2Headers headers, InetSocketAddress remote, InetSocketAddress local) {
		super(ctx, s(headers.scheme()), s(headers.method()), s(headers.path()), "HTTP/2", remote, local);
		this.headers = headers;
	}

	@Override
	public String getHeader(String name) {
		return s(headers.get(name));
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		return Collections.enumeration(headers.getAll(name).stream().map(s -> s(s)).collect(Collectors.toList()));
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(headers.names().stream().map(s -> s(s)).collect(Collectors.toList()));
	}

	@Override
	public long getDateHeader(String name) {
		return headers.getTimeMillis(name, -1);
	}

	@Override
	public int getIntHeader(String name) {
		return headers.getInt(name, -1);
	}

	@Override
	public long getContentLengthLong() {
		CharSequence s = headers.get("content-length");
		return s == null ? -1 : Long.parseLong(s.toString());
	}

	private static final String s(CharSequence s) {
		return s == null ? null : s.toString();
	}
}
