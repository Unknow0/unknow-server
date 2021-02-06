/**
 * 
 */
package unknow.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.http.HttpRawRequest.RawHeader;
import unknow.server.nio.Handler;
import unknow.server.nio.util.Buffers;

public class HttpHandler extends Handler implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(HttpHandler.class);

	private static final byte[] CRLF = new byte[] { '\r', '\n' };
	private static final byte QUESTION = '?';
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

	private boolean tryRead(byte lookup, int limit, int next, Buffers out, HttpError e) throws IOException {
		int i = pendingRead.indexOf(lookup, limit);
		if (i < 0) {
			if (pendingRead.size() > limit)
				error(e);
			return false;
		}
		pendingRead.read(i, out);
		pendingRead.skip(1);
		step = next;
		return true;
	}

	private boolean tryRead(byte[] lookup, int limit, int next, Buffers out, HttpError e) throws IOException {
		int i = pendingRead.indexOf(lookup, limit);
		if (i < 0) {
			if (pendingRead.size() > limit)
				error(e);
			return false;
		}
		pendingRead.read(i, out);
		pendingRead.skip(lookup.length);
		step = next;
		return true;
	}

	private void tryParse() throws IOException {
		if (step == METHOD)
			tryRead(SPACE, MAX_METHOD_SIZE, PATH, request.method, HttpError.BAD_REQUEST);
		if (step == PATH) {
			int m = pendingRead.indexOf(SPACE, MAX_PATH_SIZE);
			if (m < 0) {
				if (pendingRead.size() > MAX_PATH_SIZE)
					error(HttpError.URI_TOO_LONG);
				return;
			}
			int l = pendingRead.indexOf(QUESTION, m);
			int query = 0;
			if (l > 0) {
				query = m - l;
				m = l;
			}
			int i = pendingRead.read();
			if (i != '/') {
				error(HttpError.BAD_REQUEST);
				return;
			}
			m--;
			while ((i = pendingRead.indexOf((byte) '/', m)) > 0) {
				Buffers b = new Buffers();
				pendingRead.read(i, b);
				if (i == 1) {
					if (b.getHead().b[0] != '.') {
						request.path.add(b);
						b.append((byte) i);
					}
				} else if (i == 2) {
					byte[] p = b.getHead().b;
					if (p[0] != '.' || p[1] != '.')
						request.path.add(b);
					else
						request.path.remove(request.path.size() - 1);
				} else
					request.path.add(b);
				pendingRead.read();
				m -= i + 1;
			}
			if (m > 0) {
				Buffers b = new Buffers();
				pendingRead.read(m, b);
				request.path.add(b);
			}
			if (query > 0) {
				pendingRead.read();
				pendingRead.read(query, request.query);
			}
			pendingRead.read();
			step = PROTOCOL;
		}
		if (step == PROTOCOL)
			tryRead(CRLF, MAX_VERSION_SIZE, HEADER, request.protocol, HttpError.BAD_REQUEST);
		if (step == HEADER) {
			int k;
			while ((k = pendingRead.indexOf(CRLF, MAX_HEADER_SIZE)) > 0) {
				int i = pendingRead.indexOf(COLON, MAX_HEADER_SIZE);
				if (i < 0) {
					if (pendingRead.size() > MAX_HEADER_SIZE)
						error(HttpError.HEADER_TOO_LARGE);
					return;
				}

				RawHeader header = request.nextHeader();
				if (header == null) { // max number of header reach
					error(HttpError.HEADER_TOO_LARGE);
					return;
				}
				pendingRead.read(i, header);
				pendingRead.skip(1);
				// TODO trim
				pendingRead.read(pendingRead.indexOf(CRLF, MAX_HEADER_SIZE), header.value);
				pendingRead.skip(2);
			}
			if (k < 0 && pendingRead.size() > MAX_PATH_SIZE)
				error(HttpError.HEADER_TOO_LARGE);
			if (k == 0) {
				pendingRead.skip(2);
				step = CONTENT;
				request.content = pendingRead;
			}
		}
	}

	private final void error(HttpError e) throws IOException {
		log.error("{}: {}", getRemote(), e);
		getOut().write(e.data);
		getOut().close();
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