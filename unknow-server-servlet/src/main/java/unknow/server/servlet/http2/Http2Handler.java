package unknow.server.servlet.http2;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletInputStreamImpl;

public final class Http2Handler extends SimpleChannelInboundHandler<Http2StreamFrame> {
	private static final Logger logger = LoggerFactory.getLogger(Http2Handler.class);

	private static final AttributeKey<Future<?>> FUTURE = AttributeKey.valueOf(Http2Handler.class, "future");
	private static final AttributeKey<ServletInputStreamImpl> INPUT = AttributeKey.valueOf(Http2Handler.class, "input");

	private final ExecutorService pool;
	private final ServletContextImpl servletContext;

	public Http2Handler(ExecutorService pool, ServletContextImpl servletContext) {
		this.pool = pool;
		this.servletContext = servletContext;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
		logger.debug("{} evt {}", ctx.channel(), evt);
		if (evt instanceof IdleStateEvent)
			ctx.close();
		if (evt instanceof Http2ResetFrame) {
			Channel channel = ctx.channel();
			channel.attr(INPUT).set(null);
			channel.attr(FUTURE).getAndSet(null).cancel(true);
		}
	}

	@SuppressWarnings("resource")
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame msg) {
		Channel channel = ctx.channel();
		logger.debug("{} msg {}", channel, msg);
		if (msg instanceof Http2HeadersFrame) {
			InetSocketAddress remote = (InetSocketAddress) channel.remoteAddress();
			InetSocketAddress local = (InetSocketAddress) channel.localAddress();

			Http2HeadersFrame h = (Http2HeadersFrame) msg;
			Http2ServletRequest req = new Http2ServletRequest(servletContext, h.headers(), remote, local);
			Http2ServletResponse res = new Http2ServletResponse(ctx, servletContext, req, h.stream().id());
			channel.attr(FUTURE).set(pool.submit(new HttpWorker(servletContext, req, res)));
			if (h.isEndStream())
				req.rawInput().close();
			else
				channel.attr(INPUT).set(req.rawInput());
		}
		if (msg instanceof Http2DataFrame) {
			Http2DataFrame d = (Http2DataFrame) msg;

			Attribute<ServletInputStreamImpl> attr = channel.attr(INPUT);
			attr.get().add(d.content());
			if (d.isEndStream())
				attr.set(null);
		}
		if (msg instanceof Http2ResetFrame)
			channel.attr(INPUT).getAndSet(null).close();
	}
}