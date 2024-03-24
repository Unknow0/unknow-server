package unknow.server.servlet.http11;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.HttpProcessor;
import unknow.server.util.io.BuffersUtils;

public class Http11Processor implements HttpProcessor {
	private static final Logger logger = LoggerFactory.getLogger(Http11Processor.class);

	private static final byte[] END = new byte[] { '\r', '\n', '\r', '\n' };

	private static final int MAX_START_SIZE = 8192;

	private final HttpConnection co;

	private volatile Future<?> exec;

	public Http11Processor(HttpConnection co) {
		this.co = co;
	}

	@Override
	public final void process() { // nothing to do
		if (exec != null)
			exec = co.submit(new Http11Worker(co));
	}

	@Override
	public final boolean isClosed() {
		return exec == null || exec.isDone();
	}

	@Override
	public final void close() {
		if (exec != null)
			exec.cancel(true);
	}

	public static final HttpProcessorFactory Factory = co -> {
		if (BuffersUtils.indexOf(co.pendingRead, END, 0, MAX_START_SIZE) > 0)
			return new Http11Processor(co);
		return null;
	};
}
