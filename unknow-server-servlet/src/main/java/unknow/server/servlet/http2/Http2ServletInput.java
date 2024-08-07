package unknow.server.servlet.http2;

import java.io.IOException;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import unknow.server.util.io.Buffers;

public class Http2ServletInput extends ServletInputStream {
	private final Buffers in;
	private boolean closed;

	public Http2ServletInput() {
		this.in = new Buffers();
	}

	public void read(Buffers b, int size) throws InterruptedException {
		b.read(in, size, false);
	}

	@Override
	public void close() {
		try {
			in.lock();
			closed = true;
			in.signal();
		} catch (@SuppressWarnings("unused") InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			in.unlock();
		}
	}

	@Override
	public boolean isFinished() {
		return closed && in.isEmpty();
	}

	@Override
	public boolean isReady() {
		return in.length() > 0;
	}

	@Override
	public void setReadListener(ReadListener readListener) { // ok
	}

	@Override
	public int read() throws IOException {
		try {
			await();
			if (closed && in.isEmpty())
				return -1;
			return in.read(false);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		try {
			await();
			if (closed && in.isEmpty())
				return -1;
			return in.read(b, off, len, false);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

	@Override
	public int available() throws IOException {
		return in.length();
	}

	@Override
	public long skip(long n) throws IOException {
		try {
			await();
			return in.skip(n);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException(e);
		}
	}

	private void await() throws InterruptedException {
		in.lock();
		try {
			while (!closed && in.isEmpty())
				in.await();
		} finally {
			in.unlock();
		}
	}
}