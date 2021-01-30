/**
 * 
 */
package unknow.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import unknow.server.http.HttpRawRequest.RawHeader;
import unknow.server.nio.Handler;
import unknow.server.nio.util.Buffers;

public class HttpHandler extends Handler implements Runnable {
	private static final byte[] CRLF = new byte[] { '\r', '\n' };
	private static final byte SPACE = ' ';
	private static final byte COLON = ':';

	private static final int MAX_METHOD_SIZE = 10; // max size for method
	private static final int MAX_PATH_SIZE = 255;
	private static final int MAX_VERSION_SIZE = 12;
	private static final int MAX_HEADER_SIZE = 512;

	private static final int METHOD = 0;
	private static final int PATH = 1;
	private static final int PROTOCOL = 2;
	private static final int HEADER = 3;
	private static final int CONTENT = 4;

	private final ExecutorService executor;
	private final HttpRawProcessor processor;

	private final HttpRawRequest request = new HttpRawRequest();

	private int step = METHOD;

	/**
	 * create new RequestBuilder
	 */
	public HttpHandler(ExecutorService executor, HttpRawProcessor processor) {
		this.executor = executor;
		this.processor = processor;
	}

	@Override
	protected void handle(InputStream in, OutputStream out) {
		if (step == CONTENT)
			return;
		try {
			tryParse();
		} catch (IOException e) { // OK
		}
		if (step == CONTENT)
			executor.submit(this);
	}

	private boolean tryRead(byte lookup, int limit, int next, Buffers out) throws IOException {
		int i = pendingRead.indexOf(lookup, limit);
		if (i < 0) {
			if (pendingRead.size() > limit)
				getOut().close();
			return false;
		}
		pendingRead.read(i, out);
		pendingRead.skip(1);
		step = next;
		return true;
	}

	private boolean tryRead(byte[] lookup, int limit, int next, Buffers out) throws IOException {
		int i = pendingRead.indexOf(lookup, limit);
		if (i < 0) {
			if (pendingRead.size() > limit)
				getOut().close();
			return false;
		}
		pendingRead.read(i, out);
		pendingRead.skip(lookup.length);
		step = next;
		return true;
	}

	private void tryParse() throws IOException {
		if (step == METHOD)
			tryRead(SPACE, MAX_METHOD_SIZE, PATH, request.method);
		if (step == PATH)
			tryRead(SPACE, MAX_PATH_SIZE, PROTOCOL, request.path);
		if (step == PROTOCOL)
			tryRead(CRLF, MAX_VERSION_SIZE, HEADER, request.protocol);
		if (step == HEADER) {
			int k;
			while ((k = pendingRead.indexOf(CRLF, MAX_HEADER_SIZE)) > 0) {
				int i = pendingRead.indexOf(COLON, MAX_HEADER_SIZE);
				if (i < 0) {
					if (pendingRead.size() > MAX_HEADER_SIZE)
						getOut().close();
					return;
				}

				RawHeader header = request.nextHeader();
				if (header == null) { // max number of header reach
					getOut().close();
					return;
				}
				pendingRead.read(i, header);
				pendingRead.skip(1);
				// TODO trim
				pendingRead.read(pendingRead.indexOf(CRLF, MAX_HEADER_SIZE), header.value);
				pendingRead.skip(2);
			}
			if (k == 0) {
				pendingRead.skip(2);
				step = CONTENT;
				request.content = pendingRead;
			}
		}
	}

	@Override
	public void run() {
		try {
			request.remote = getRemote();
			request.local = getLocal();
			processor.process(request, getOut());
		} catch (IOException e) {
			try {
				getOut().close();
			} catch (IOException e1) {
				e.addSuppressed(e1);
			}
			e.printStackTrace();
		}
	}

	@Override
	public void reset() {
		super.reset();
		step = METHOD;
		request.reset();
	}
}