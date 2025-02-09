package unknow.server.servlet.http2;

import java.io.IOException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import unknow.server.servlet.impl.AbstractServletOutput;

public class Http2ServletOutput extends AbstractServletOutput<Http2ServletResponse> {

	protected Http2ServletOutput(ChannelHandlerContext out, Http2ServletResponse res) {
		super(out, res);
	}

	@Override
	protected void writebuffer() throws IOException {
		System.out.println(isClosed());
		out.write(new DefaultHttp2DataFrame(buffer.copy(), isClosed(), res.streamId));
		buffer.clear();
	}
}
