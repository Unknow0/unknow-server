package unknow.server.servlet.http2;

import java.io.IOException;
import java.nio.ByteBuffer;

import unknow.server.servlet.impl.AbstractServletOutput;
import unknow.server.servlet.impl.ServletResponseImpl;

public class Http2ServletOutput extends AbstractServletOutput {
	private final Http2Processor p;
	private final int id;

	public Http2ServletOutput(ServletResponseImpl res, Http2Processor p, int id) {
		super(res);
		this.p = p;
		this.id = id;
	}

	public boolean isDone() {
		return isClosed() && buffer.position() == 0;
	}

	@Override
	protected void writeBuffer(ByteBuffer b) throws IOException {
		p.sendData(id, b, isClosed());
	}
}