package unknow.server.servlet.http2;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.util.collection.IntObjectHashMap;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletInputStreamImpl;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.impl.ServletResponseImpl;

public final class Http2Handler extends SimpleChannelInboundHandler<Http2Frame> {
	private static final Logger logger = LoggerFactory.getLogger(Http2Handler.class);

	private final ExecutorService pool;
	private final ServletContextImpl servletContext;
	private final int keepAlive;

	private final IntObjectHashMap<Http2Stream> streams;

	private Future<?> keepAliveTimeout = CompletableFuture.completedFuture(null);

	public Http2Handler(ExecutorService pool, ServletContextImpl servletContext, int keepAlive) {
		this.pool = pool;
		this.servletContext = servletContext;
		this.keepAlive = keepAlive;

		this.streams = new IntObjectHashMap<>();
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		logger.info("{} active", ctx.channel());
		ctx.pipeline().addBefore("http2", "http2outbound", new Outbound());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("{} inactive", ctx.channel());
		keepAliveTimeout.cancel(true);
		for (Http2Stream s : streams.values())
			s.cancel();
		ctx.fireChannelInactive();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
		logger.debug("{} evt {}", ctx.channel(), evt);
//		if (evt instanceof Http2ResetFrame) {
//			input = null;
//			f.cancel(true);
//		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Http2Frame msg) {
		Channel channel = ctx.channel();
		logger.debug("{} msg {}", channel, msg);

		if (msg instanceof Http2HeadersFrame) {
			InetSocketAddress remote = (InetSocketAddress) channel.remoteAddress();
			InetSocketAddress local = (InetSocketAddress) channel.localAddress();

			Http2HeadersFrame h = (Http2HeadersFrame) msg;
			Http2ServletRequest req = new Http2ServletRequest(servletContext, h.headers(), remote, local);
			Http2ServletResponse res = new Http2ServletResponse(ctx, servletContext, req, h.stream());

			streams.put(h.stream().id(), new Http2Stream(servletContext, req, res));
			if (h.isEndStream())
				req.rawInput().close();
		}
		if (msg instanceof Http2DataFrame) {
			Http2DataFrame d = (Http2DataFrame) msg;
			Http2Stream http2Stream = streams.get(d.stream().id());
			if (http2Stream != null) {
				http2Stream.add(d.content());
				if (d.isEndStream()) {
					http2Stream.close();
				}
			}
		}
		if (msg instanceof Http2ResetFrame) {
			streams.remove(((Http2ResetFrame) msg).stream().id()).cancel();
		}
	}

	private class Http2Stream extends HttpWorker {

		private final Future<?> f;
		private final ServletInputStreamImpl input;

		public Http2Stream(ServletContextImpl ctx, ServletRequestImpl req, ServletResponseImpl res) {
			super(ctx, req, res);
			f = pool.submit(this);
			input = req.rawInput();
		}

		public void add(ByteBuf b) {
			if (!f.isDone())
				input.add(b);
		}

		public void close() {
			if (!f.isDone())
				input.close();
		}

		public void cancel() {
			f.cancel(true);
			input.close();
		}
	}

	private class Outbound extends ChannelOutboundHandlerAdapter implements Runnable {
		private ChannelHandlerContext ctx;
		private boolean closing = false;

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			this.ctx = ctx;
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			logger.debug("{} writ {}", ctx.channel(), msg);

			if (msg instanceof Http2DataFrame) {
				Http2DataFrame f = (Http2DataFrame) msg;
				if (f.isEndStream())
					streams.remove(f.stream().id());
			} else if (msg instanceof Http2HeadersFrame) {
				Http2HeadersFrame f = (Http2HeadersFrame) msg;
				if (f.isEndStream())
					streams.remove(f.stream().id());
			} else if (msg instanceof Http2ResetFrame)
				streams.remove(((Http2ResetFrame) msg).stream().id());
			ChannelFuture write = ctx.write(msg, promise);
			if (streams.isEmpty()) {
				if (closing) {
					write.addListener(ChannelFutureListener.CLOSE);
				} else if (keepAlive > 0)
					keepAliveTimeout = ctx.executor().schedule(this, keepAlive, TimeUnit.SECONDS);
			}
		}

		@Override
		public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
			logger.info("{} closing", ctx.channel());
			if (streams.isEmpty())
				ctx.close(promise);
			else
				closing = true;
		}

		@Override
		public void run() {
			logger.info("{} keep-alive reached", ctx.channel());
			ctx.close();
		}
	}
}