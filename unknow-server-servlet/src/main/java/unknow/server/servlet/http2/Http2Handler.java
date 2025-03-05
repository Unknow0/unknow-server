package unknow.server.servlet.http2;

import java.net.InetSocketAddress;
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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Frame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2ResetFrame;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.collection.IntObjectHashMap;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletInputStreamImpl;
import unknow.server.servlet.impl.ServletRequestImpl;
import unknow.server.servlet.impl.ServletResponseImpl;

public final class Http2Handler extends ChannelInboundHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(Http2Handler.class);

	private final ExecutorService pool;
	private final ServletContextImpl servletContext;
	private final int keepAlive;

	private final IntObjectHashMap<Http2Stream> streams;

	private Future<?> keepAliveTimeout;

	public Http2Handler(ExecutorService pool, ServletContextImpl servletContext, int keepAlive) {
		this.pool = pool;
		this.servletContext = servletContext;
		this.keepAlive = keepAlive;

		this.streams = new IntObjectHashMap<>();
	}

	public ChannelOutboundHandlerAdapter outbound() {
		return new Outbound();
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
		logger.info("{} evt {}", ctx.channel(), evt);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		boolean release = true;
		try {
			logger.info("{} msg {} {}", ctx.channel(), streams.size(), msg);
			if (msg instanceof Http2HeadersFrame)
				handleHeader(ctx, (Http2HeadersFrame) msg);
			else if (msg instanceof Http2DataFrame)
				handleData((Http2DataFrame) msg);
			else if (msg instanceof Http2ResetFrame) {
				Http2Stream remove = streams.remove(((Http2ResetFrame) msg).stream().id());
				if (remove != null)
					remove.cancel();
			} else if (!(msg instanceof Http2Frame)) {
				release = false;
				ctx.fireChannelRead(msg);
			}
		} finally {
			if (release)
				ReferenceCountUtil.release(msg);
		}
	}

	private void handleHeader(ChannelHandlerContext ctx, Http2HeadersFrame msg) {
		Channel channel = ctx.channel();
		InetSocketAddress remote = (InetSocketAddress) channel.remoteAddress();
		InetSocketAddress local = (InetSocketAddress) channel.localAddress();

		Http2ServletRequest req = new Http2ServletRequest(servletContext, msg.headers(), remote, local);
		Http2ServletResponse res = new Http2ServletResponse(ctx, servletContext, req, msg.stream());

		streams.put(msg.stream().id(), new Http2Stream(servletContext, req, res));
		if (msg.isEndStream())
			req.rawInput().close();
	}

	private void handleData(Http2DataFrame msg) {
		Http2Stream http2Stream = streams.get(msg.stream().id());
		if (http2Stream != null) {
			http2Stream.add(msg.content());
			if (msg.isEndStream()) {
				http2Stream.close();
			}
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
		private boolean closing = false;
		private ChannelHandlerContext ctx;

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			logger.info("{} active", ctx.channel());
			this.ctx = ctx;
			keepAliveTimeout = ctx.executor()
					.schedule(() -> ctx.writeAndFlush(new DefaultHttp2GoAwayFrame(Http2Error.SETTINGS_TIMEOUT)).addListener(ChannelFutureListener.CLOSE), 2, TimeUnit.SECONDS);
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			logger.debug("{} writ {}", ctx.channel(), msg);

			if (msg instanceof DefaultHttp2DataFrame) {
				Http2DataFrame f = (Http2DataFrame) msg;
				if (f.isEndStream())
					streams.remove(f.stream().id());
			} else if (msg instanceof DefaultHttp2HeadersFrame) {
				Http2HeadersFrame f = (Http2HeadersFrame) msg;
				if (f.isEndStream())
					streams.remove(f.stream().id());
			} else if (msg instanceof DefaultHttp2ResetFrame)
				streams.remove(((Http2ResetFrame) msg).stream().id());

			ChannelFuture write = ctx.write(msg, promise);
			if (streams.isEmpty()) {
				if (closing) {
					write.addListener(ChannelFutureListener.CLOSE);
				} else if (keepAlive > 0) {
					keepAliveTimeout.cancel(true);
					keepAliveTimeout = ctx.executor().schedule(this, keepAlive, TimeUnit.SECONDS);
				}
			}
		}

		@Override
		public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
			logger.info("{} closing", ctx.channel());
			keepAliveTimeout.cancel(true);
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