package unknow.server.servlet.http11;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.HttpProcessor;
import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersUtils;

/**
 * http/1.1 implementation
 */
public class Http11Processor implements HttpProcessor {
	private static final byte[] END = new byte[] { '\r', '\n', '\r', '\n' };

	private static final int MAX_START_SIZE = 8192;

	private final HttpConnection co;

	private volatile Future<?> exec;

	/**
	 * new http11 processor
	 * 
	 * @param co the connection
	 */
	public Http11Processor(HttpConnection co, boolean start) {
		this.co = co;
		this.exec = start ? co.submit(new Http11Worker(co)) : CompletableFuture.completedFuture(null);
	}

	@Override
	public final void process() {
		if (exec.isDone() && isStart(co.pendingRead()))
			exec = co.submit(new Http11Worker(co));
	}

	@Override
	public final boolean isClosable(boolean stop) {
		process();
		return exec.isDone();
	}

	@Override
	public final void close() {
		exec.cancel(true);
	}

	public static final boolean isStart(Buffers b) {
		try {
			return BuffersUtils.indexOf(b, END, 0, MAX_START_SIZE) > 0;
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/** the processor factory */
	public static final HttpProcessorFactory Factory = co -> {
		if (isStart(co.pendingRead()))
			return new Http11Processor(co, true);
		return null;
	};
}
