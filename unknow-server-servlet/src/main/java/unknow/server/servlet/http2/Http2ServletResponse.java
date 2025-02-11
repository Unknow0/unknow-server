package unknow.server.servlet.http2;

import java.util.Collection;
import java.util.stream.Collectors;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletResponseImpl;

public class Http2ServletResponse extends ServletResponseImpl {

	private final Http2Headers headers;

	private Http2ServletOutput rawOutput;

	public Http2ServletResponse(ChannelHandlerContext out, ServletContextImpl ctx, Http2ServletRequest req) {
		super(out, ctx, req);
		headers = new DefaultHttp2Headers();
		headers.status("200");
		rawOutput = new Http2ServletOutput(out, this);
	}

	@Override
	protected Http2ServletOutput rawOutput() {
		return rawOutput;
	}

	@Override
	protected void doCommit() throws InterruptedException {
		out.write(new DefaultHttp2HeadersFrame(headers, rawOutput.isClosed() && rawOutput.remaingSize() == 0));
	}

	@Override
	protected void doReset(boolean full) {
		if (full)
			headers.clear();
		headers.status("200");
	}

	@Override
	public String getHeader(String name) {
		return s(headers.get(name));
	}

	@Override
	public Collection<String> getHeaders(String name) {
		return headers.getAll(name).stream().map(s -> s(s)).collect(Collectors.toList());
	}

	@Override
	public Collection<String> getHeaderNames() {
		return headers.names().stream().map(s -> s(s)).collect(Collectors.toList());
	}

	@Override
	public boolean containsHeader(String name) {
		return headers.contains(name);
	}

	@Override
	public void setHeader(String name, String value) {
		headers.set(name.toLowerCase(), value);
	}

	@Override
	public void addHeader(String name, String value) {
		headers.add(name.toLowerCase(), value);
	}

	@Override
	public void setStatus(int sc) {
		headers.status(Integer.toString(sc));
	}

	@Override
	public int getStatus() {
		return headers.getInt(":status", 200);
	}

	private static final String s(CharSequence s) {
		return s == null ? null : s.toString();
	}
}
