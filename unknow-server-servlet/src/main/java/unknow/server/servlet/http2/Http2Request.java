package unknow.server.servlet.http2;

import java.nio.ByteBuffer;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletInputStream;
import unknow.server.servlet.HttpConnection;
import unknow.server.servlet.impl.ServletRequestImpl;

public class Http2Request extends ServletRequestImpl {
	private final Http2ServletInput in;

	public Http2Request(HttpConnection co, DispatcherType type) {
		super(co, type);
		in = new Http2ServletInput();
		setProtocol("HTTP/2");
	}

	@Override
	protected ServletInputStream createInput() {
		return in;
	}

	public void append(ByteBuffer buf) {
		in.append(buf);
	}

	public void close() {
		in.close();
	}

}
