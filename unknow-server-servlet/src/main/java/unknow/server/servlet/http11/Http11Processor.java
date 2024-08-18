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

	private volatile Future<?> exec = CompletableFuture.completedFuture(null);

	/**
	 * new http11 processor
	 * @param co the connection
	 */
	public Http11Processor(HttpConnection co) {
		this.co = co;
	}

	@Override
	public final void process() throws InterruptedException {
		if (exec.isDone() && isStart(co.pendingRead()))
			exec = co.submit(new Http11Worker(co));
	}

	@Override
	public final boolean isClosable(boolean stop) {
		if (!exec.isDone())
			return false;
		if (co.pendingRead().isEmpty())
			return true;
		exec = co.submit(new Http11Worker(co));
		return false;
	}

	@Override
	public final void close() {
		exec.cancel(true);
	}

	public static final boolean isStart(Buffers b) throws InterruptedException {
		return BuffersUtils.indexOf(b, END, 0, MAX_START_SIZE) > 0;
	}

	/** the processor factory */
	public static final HttpProcessorFactory Factory = co -> {
		if (isStart(co.pendingRead()))
			return new Http11Processor(co);
		return null;
	};
}
