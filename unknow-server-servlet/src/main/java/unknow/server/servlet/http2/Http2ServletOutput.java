package unknow.server.servlet.http2;

import java.io.IOException;

import unknow.server.servlet.impl.ServletResponseImpl;
import unknow.server.servlet.impl.out.AbstractServletOutput;

public class Http2ServletOutput extends AbstractServletOutput {
	private final Http2Processor p;
	private final int id;

	public Http2ServletOutput(ServletResponseImpl res, Http2Processor p, int id) {
		super(res);
		this.p = p;
		this.id = id;
	}

	public boolean isDone() {
		return isClosed() && buffer.isEmpty();
	}

	@Override
	public void flush() throws IOException {
		res.commit();
		try {
			p.sendData(id, buffer, isClosed());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

}