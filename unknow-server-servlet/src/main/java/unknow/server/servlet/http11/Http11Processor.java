package unknow.server.servlet.http11;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.HttpProcessor;
import unknow.server.util.io.Buffers;
import unknow.server.util.io.Buffers.WalkResult;
import unknow.server.util.io.BuffersUtils;
import unknow.server.util.io.BuffersUtils.IndexOfBloc;

/**
 * http/1.1 implementation
 */
public class Http11Processor implements HttpProcessor {
	private static final byte[] END = new byte[] { '\r', '\n', '\r', '\n' };

	private static final int MAX_START_SIZE = 8192;

	private final HttpConnection co;

	private final IndexOfBloc w;

	private volatile Future<?> exec = CompletableFuture.completedFuture(null);

	/**
	 * new http11 processor
	 * @param co the connection
	 */
	public Http11Processor(HttpConnection co) {
		this.co = co;
		this.w = new IndexOfBloc(END);
	}

	@Override
	public final void process() {
		if (exec.isDone() && canProcess())
			exec = co.submit(new Http11Worker(co));
	}

	@Override
	public final boolean isClosable(boolean stop) {
//		process();
		return exec.isDone();
	}

	@Override
	public final void close() {
		exec.cancel(true);
	}

	private final boolean canProcess() {
		this.w.reset();
		try {
			return co.pendingRead().walk(w, 0, MAX_START_SIZE) == WalkResult.STOPED;
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
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
