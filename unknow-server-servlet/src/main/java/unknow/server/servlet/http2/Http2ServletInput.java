package unknow.server.servlet.http2;

import java.io.IOException;
import java.nio.ByteBuffer;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import unknow.server.util.io.ByteBufferInputStream;

public class Http2ServletInput extends ServletInputStream {
	private final ByteBufferInputStream in;

	public Http2ServletInput() {
		this.in = new ByteBufferInputStream();
	}

	public void append(ByteBuffer b) {
		in.addBuffer(b);
	}

	@Override
	public void close() {
		in.close();
	}

	@Override
	public boolean isFinished() {
		return in.isClosed();
	}

	@Override
	public boolean isReady() {
		return in.available() > 0;
	}

	@Override
	public void setReadListener(ReadListener readListener) { // ok
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}

	@Override
	public int available() throws IOException {
		return in.available();
	}

	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}
}