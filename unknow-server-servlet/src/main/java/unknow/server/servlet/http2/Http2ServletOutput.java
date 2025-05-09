package unknow.server.servlet.http2;

import java.io.IOException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.Http2FrameStream;
import unknow.server.servlet.impl.AbstractServletOutput;

public class Http2ServletOutput extends AbstractServletOutput<Http2ServletResponse> {

	private final Http2FrameStream stream;

	protected Http2ServletOutput(ChannelHandlerContext out, Http2ServletResponse res, Http2FrameStream stream) {
		super(out, res);
		this.stream = stream;
	}

	@Override
	protected void writebuffer() throws IOException {
		DefaultHttp2DataFrame h = new DefaultHttp2DataFrame(buffer, isClosed()).stream(stream);
		if (h.isEndStream())
			ctx.writeAndFlush(h);
		else
			ctx.write(h);
		buffer = ctx.alloc().buffer(getBufferSize() < 8192 ? 8192 : getBufferSize());
	}

	@Override
	public String toString() {
		return stream.toString();
	}
}
