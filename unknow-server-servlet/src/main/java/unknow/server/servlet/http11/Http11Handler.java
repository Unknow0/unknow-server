package unknow.server.servlet.http11;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateEvent;
import unknow.server.servlet.HttpWorker;
import unknow.server.servlet.impl.ServletContextImpl;
import unknow.server.servlet.impl.ServletInputStreamImpl;

public final class Http11Handler extends SimpleChannelInboundHandler<HttpObject> {
	private static final Logger logger = LoggerFactory.getLogger(Http11Handler.class);
	private static final ExecutorService POOL = Executors.newCachedThreadPool();

	private final ServletContextImpl servletContext;
	private final OrderedLock lock;

	private ServletInputStreamImpl input;

	public Http11Handler(ServletContextImpl servletContext) {
		this.servletContext = servletContext;
		this.lock = new OrderedLock();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
		if (evt instanceof IdleStateEvent)
			ctx.close();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
		logger.debug("{} {}", ctx, msg);

		if (msg instanceof HttpRequest) {
			if (msg.decoderResult().isFailure())
				ctx.close();
			else {
				Channel channel = ctx.channel();
				InetSocketAddress remote = (InetSocketAddress) channel.remoteAddress();
				InetSocketAddress local = (InetSocketAddress) channel.localAddress();

				String scheme = ctx.pipeline().get(SslHandler.class) == null ? "http" : "https";
				Http11ServletRequest req = new Http11ServletRequest(servletContext, scheme, (HttpRequest) msg, remote, local);
				Http11ServletResponse res = new Http11ServletResponse(ctx, servletContext, req, lock, lock.nextId());
				input = req.rawInput();
				POOL.submit(new HttpWorker(servletContext, req, res));
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
		cause.printStackTrace();
		ctx.close();
	}

	public static class OrderedLock {
		private final ReentrantLock lock;
		private final Condition cond;

		private int id;
		private int nextId;

		public OrderedLock() {
			lock = new ReentrantLock();
			cond = lock.newCondition();
		}

		public int nextId() {
			return nextId++;
		}

		public void unlockNext() {
			lock.lock();
			try {
				id++;
				cond.signalAll();
			} finally {
				lock.unlock();
			}
		}

		public void waitUntil(int i) throws InterruptedException {
			lock.lock();
			try {
				while (id < i)
					cond.await();
			} finally {
				lock.unlock();
			}
		}
	}
}