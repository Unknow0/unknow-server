/**
 * 
 */
package unknow.server.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import unknow.server.nio.util.Buffers;

public class DefaultHttpProcessor implements HttpRawProcessor {
	private static final byte[] ERROR = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\nContent-Length: 0\r\n\r\n".getBytes();
	private static final byte PARAM = '?';

	private final Path root;

	private final Buffers tmp = new Buffers();

	public DefaultHttpProcessor(Path root) {
		this.root = root;
	}

	@Override
	public void process(HttpRawRequest request, OutputStream out) throws IOException {
		// TODO check file
		System.out.println(request);
		out.write(ERROR);
		out.close();
	}
}