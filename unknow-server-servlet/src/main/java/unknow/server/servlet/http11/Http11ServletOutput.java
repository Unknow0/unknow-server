package unknow.server.servlet.http11;

import java.io.IOException;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import unknow.server.servlet.impl.AbstractServletOutput;

public class Http11ServletOutput extends AbstractServletOutput<Http11ServletResponse> {

	protected Http11ServletOutput(ChannelHandlerContext out, Http11ServletResponse res) {
		super(out, res);
	}

	@Override
	protected void writebuffer() throws IOException {
		out.write(new DefaultHttpContent(buffer.copy()));
		buffer.clear();
	}

	@Override
	protected void afterClose() throws IOException {
		ChannelFuture f = out.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		if ("close".equals(res.getHeader("connection")))
			f.addListener(ChannelFutureListener.CLOSE);
		res.lock.unlockNext();
	}
}
