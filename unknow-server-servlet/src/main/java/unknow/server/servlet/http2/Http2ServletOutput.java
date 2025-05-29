package unknow.server.servlet.http2;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.servlet.impl.AbstractServletOutput;
import unknow.server.servlet.impl.ServletResponseImpl;

public class Http2ServletOutput extends AbstractServletOutput {
	private final Http2Processor p;
	private final int id;
	private boolean endStreamSend;

	public Http2ServletOutput(ServletResponseImpl res, Http2Processor p, int id) {
		super(res, 9);
		this.p = p;
		this.id = id;
		this.endStreamSend = false;
	}

	public boolean isDone() {
		return isClosed() && isEmpty();
	}

	@Override
	protected void afterClose() throws IOException {
		if (!endStreamSend)
			p.sendData(id, null, true);
	}

	@Override
	protected void writeBuffer(ByteBuffer b) throws IOException {
		boolean closed = isClosed();
		p.sendData(id, b, closed);
		if (closed)
			endStreamSend = true;
	}
}