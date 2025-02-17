package unknow.server.servlet.http11;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.ReferenceCountUtil;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletInputStreamImpl;
import unknow.server.servlet.utils.OrderedLock;

public final class Http11Handler extends ChannelInboundHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(Http11Handler.class);

	private final ExecutorService pool;
	private final ServletContextImpl servletContext;
	private final OrderedLock lock;

	private final int keepAlive;

	private Http11ServletResponse res;
	private ServletInputStreamImpl input;
	private Future<?> f = CompletableFuture.completedFuture(null);
	private Future<?> keepAliveTimeout = CompletableFuture.completedFuture(null);

	public Http11Handler(ExecutorService pool, ServletContextImpl servletContext, int keepAlive) {
		this.pool = pool;
		this.servletContext = servletContext;
		this.lock = new OrderedLock();

		this.keepAlive = keepAlive;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.info("{} active", ctx.channel());
		ctx.pipeline().addBefore("http11", "http1outbound", new Outbound());
		ctx.fireChannelActive();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("{} inactive", ctx.channel());
		keepAliveTimeout.cancel(true);
		f.cancel(true);
		ctx.fireChannelInactive();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
		logger.info("{} evt {}", ctx.channel(), evt);
		ctx.fireUserEventTriggered(evt);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		boolean release = true;
		try {
			if (msg instanceof HttpObject)
				channelRead0(ctx, (HttpObject) msg);
			else {
				release = false;
				ctx.fireChannelRead(msg);
			}
		} catch (Exception e) {
			logger.error("{} Failed to process {}", ctx.channel(), msg, e);
			ctx.close();
		} finally {
			if (release)
				ReferenceCountUtil.release(msg);
		}
	}

	private void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
		logger.debug("{} read {}", ctx.channel(), msg);
		keepAliveTimeout.cancel(true);

		if (msg.decoderResult().isFailure()) {
			ctx.close();
			return;
		}

		if (msg instanceof HttpRequest) {
			if (msg.decoderResult().isFailure())
				ctx.close();
			else {
				Channel channel = ctx.channel();
				InetSocketAddress remote = (InetSocketAddress) channel.remoteAddress();
				InetSocketAddress local = (InetSocketAddress) channel.localAddress();

				String scheme = ctx.pipeline().get(SslHandler.class) == null ? "http" : "https";
				Http11ServletRequest req = new Http11ServletRequest(servletContext, scheme, (HttpRequest) msg, remote, local);
				boolean connectionClose = keepAlive < 0 || "close".equals(req.getHeader("connection"));
				res = new Http11ServletResponse(ctx, servletContext, req, lock, lock.nextId(), connectionClose);
				input = req.rawInput();
				f = pool.submit(new HttpWorker(servletContext, req, res));
			}
		}

		if (msg instanceof HttpContent)
			input.add(((HttpContent) msg).content());
		if (msg instanceof LastHttpContent) {
			input.close();
			input = null;
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.error("{}", ctx.channel(), cause);
		ctx.close();
	}

	public class Outbound extends ChannelOutboundHandlerAdapter implements Runnable {
		private ChannelHandlerContext ctx;
		private boolean closing = false;

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			this.ctx = ctx;
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			logger.debug("{} writ {}", ctx.channel(), msg);
			if (msg instanceof LastHttpContent) {
				if (closing)
					ctx.close();
				else if (keepAlive > 0)
					keepAliveTimeout = ctx.executor().schedule(this, keepAlive, TimeUnit.SECONDS);
			}
			ctx.write(msg, promise);
		}

		@Override
		public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
			logger.info("{} closing", ctx.channel());
			closing = true;
			if (f.isDone())
				ctx.close(promise);
			else
				res.setConnectionClose();
		}

		@Override
		public void run() {
			logger.info("{} keep-alive reached", ctx.channel());
			ctx.close();
		}
	}
}