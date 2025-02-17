package unknow.server.servlet.impl;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

public class ServletInputStreamImpl extends ServletInputStream {

	private final CompositeByteBuf buffers;
	private final ReentrantLock lock;
	private final Condition cond;
	private boolean closed;

	public ServletInputStreamImpl() {
		buffers = Unpooled.compositeBuffer();
		lock = new ReentrantLock();
		cond = lock.newCondition();
	}

	public void add(ByteBuf b) {
		lock.lock();
		try {
			if (closed)
				return;
			buffers.addComponent(true, b.retain());
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void release() {
		close();
		buffers.release();
	}

	@Override
	public void close() {
		lock.lock();
		try {
			closed = true;
			cond.signalAll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isFinished() {
		lock.lock();
		try {
			return closed && buffers.isReadable();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isReady() {
		try {
			return !closed && buffers.isReadable();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void setReadListener(ReadListener readListener) {
		// TODO Auto-generated method stub
	}

	@Override
	public int read() throws IOException {
		lock.lock();
		try {
			while (!closed && !buffers.isReadable())
				cond.await();
			return !buffers.isReadable() ? -1 : buffers.readByte();
		} catch (InterruptedException e) {
			throw new IOException(e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		lock.lock();
		try {
			while (!closed && !buffers.isReadable())
				cond.await();
			if (!buffers.isReadable())
				return -1;
			len = Math.min(len, buffers.readableBytes());
			buffers.readBytes(b, off, len);
			return len;
		} catch (InterruptedException e) {
			throw new IOException(e);
		} finally {
			lock.unlock();
		}
	}
}
