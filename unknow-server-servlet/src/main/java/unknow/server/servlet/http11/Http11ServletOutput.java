package unknow.server.servlet.http11;

import java.io.IOException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import unknow.server.servlet.impl.AbstractServletOutput;

public class Http11ServletOutput extends AbstractServletOutput<Http11ServletResponse> {

	protected Http11ServletOutput(ChannelHandlerContext out, Http11ServletResponse res) {
		super(out, res);
	}

	@Override
	protected void writebuffer() throws IOException {
		HttpContent c;
		if (isClosed()) {
			c = new DefaultLastHttpContent(buffer.copy());
		} else
			c = new DefaultHttpContent(buffer.copy());
		System.out.println(c);
		out.write(c);
		buffer.clear();
	}

	@Override
	protected void afterClose() throws IOException {
		out.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		res.lock.unlockNext();
	}
}
