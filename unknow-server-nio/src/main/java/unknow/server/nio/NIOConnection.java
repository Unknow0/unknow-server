/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLEngine;

import unknow.server.nio.NIOWorker.WorkerTask;
import unknow.server.util.io.ByteBuffers;

/**
 * used to handle raw data
 * 
 * @author unknow
 */
public final class NIOConnection extends NIOHandlerDelegate {
	private static final InetSocketAddress DISCONECTED = InetSocketAddress.createUnresolved("", 0);

	/** Output stream */
	protected final Out out;

	protected final NIOWorker worker;

	/** selection key */
	protected final SelectionKey key;
	protected final SocketChannel channel;

	private final AtomicBoolean writeScheduled;
	private final WriteCheck writeCheck;

	private InetSocketAddress local;
	private InetSocketAddress remote;

	final Queue<ByteBuffer> pending;
	final ByteBuffers writes;

	long lastCheck;
	long lastAction;
	NIOConnection next;
	NIOConnection prev;

	/**
	 * create new connection
	 * 
	 * @param worker the worker
	 * @param key the selectionKey
	 * @param handler the handler
	 */
	public NIOConnection(NIOWorker worker, SelectionKey key, NIOConnectionHandler handler) {
		super(handler);
		this.worker = worker;
		this.key = key;
		this.channel = (SocketChannel) key.channel();
		this.writeScheduled = new AtomicBoolean(false);
		this.writeCheck = new WriteCheck();
		this.out = new Out(this);
		this.pending = new ConcurrentLinkedQueue<>();
		this.writes = new ByteBuffers(16);
	}

	public final <T> Future<T> submit(Runnable r) {
		return worker.submit(r);
	}

	public final void execute(WorkerTask task) {
		worker.execute(task);
	}

	@Override
	public boolean hasPendingWrites() {
		return !writes.isEmpty() || !pending.isEmpty() || handler.hasPendingWrites();
	}

	public long lastAction() {
		return lastAction;
	}

	@Override
	public void init(NIOConnection co, long now, SSLEngine sslEngine) throws IOException {
		lastAction = now;
		handler.init(co, now, sslEngine);
	}

	/**
	 * add a buffers to the writing queue
	 * 
	 * @param buf buffer to be written
	 * @throws InterruptedException in case of interruption
	 * @throws IOException
	 */
	public final void write(ByteBuffer buf) throws IOException {
		if (!key.isValid())
			throw new IOException("already closed");
		pending.offer(buf);
		if (writeScheduled.compareAndSet(false, true))
			execute(writeCheck);
	}

	/**
	 * @return the current outputStream
	 */
	public final Out getOut() {
		return out;
	}

	/**
	 * the remote address of the channel is connected to
	 * 
	 * @return the remote address
	 */
	public final InetSocketAddress getRemote() {
		if (remote == null) {
			try {
				remote = (InetSocketAddress) channel.getRemoteAddress();
			} catch (@SuppressWarnings("unused") Exception e) {
				remote = DISCONECTED;
			}
		}
		return remote;
	}

	/**
	 * the local address of the channel this handler has
	 * 
	 * @return the local address
	 */
	public final InetSocketAddress getLocal() {
		if (local == null) {
			try {
				local = (InetSocketAddress) channel.getLocalAddress();
			} catch (@SuppressWarnings("unused") Exception e) {
				local = DISCONECTED;
			}
		}
		return local;
	}

	/**
	 * check if this connection is closed
	 * 
	 * @return true if we should close this handler
	 */
	public boolean isClosed() {
		return out.isClosed() && !hasPendingWrites();
	}

	protected final void beforeWrite(long now) throws IOException {
		ByteBuffer b;
		while ((b = pending.poll()) != null)
			writes.accept(b);
		handler.transformWrite(writes, now);
	}

	@Override
	public final void onWrite(long now) throws IOException {
		lastAction = now;
		writes.compact();
		if (!hasPendingWrites()) {
			writeScheduled.set(false);
			if (!hasPendingWrites())
				key.interestOpsAnd(~SelectionKey.OP_WRITE);
			else if (writeScheduled.compareAndSet(false, true))
				key.interestOpsOr(SelectionKey.OP_WRITE);
		}
		handler.onWrite(now);
	}

	@Override
	public final void onRead(ByteBuffer b, long now) throws IOException {
		lastAction = now;
		handler.onRead(b, now);
	}

	@Override
	public void startClose(long now) {
		lastAction = now;
		handler.startClose(now);
	}

	/**
	 * free the handler
	 */
	@Override
	public final void doneClosing() {
		out.h = null;
		key.cancel();
		try {
			key.channel().close();
		} catch (@SuppressWarnings("unused") IOException e) { // ignore
		}
		handler.doneClosing();
	}

	@Override
	public String toString() {
		return getClass() + "[local=" + getLocal() + " remote=" + getRemote() + "] writes: " + hasPendingWrites();
	}

	/** output stream for this connection */
	public static final class Out extends OutputStream {
		private static final int BUF_SIZE = 16 * 1024;
		private NIOConnection h;

		private ByteBuffer buf;

		private Out(NIOConnection h) {
			this.h = h;
			this.buf = ByteBuffer.allocate(BUF_SIZE);
		}

		@Override
		public synchronized void write(int b) throws IOException {
			if (h == null)
				throw new IOException("already closed");
			buf.put((byte) b);
			if (!buf.hasRemaining())
				writeBuffer();
		}

		@Override
		public final void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			if (len == 0)
				return;
			if ((off | len | (off + len) | (b.length - (off + len))) < 0)
				throw new IndexOutOfBoundsException();
			if (h == null)
				throw new IOException("already closed");

			int r = buf.remaining();
			if (len > r) {
				buf.put(b, off, r);
				len -= r;
				off += r;
				writeBuffer();
				if (len < BUF_SIZE)
					buf.put(b, off, len);
				else
					h.write(ByteBuffer.wrap(Arrays.copyOfRange(b, off, off + len)));
			} else if (len == r) {
				buf.put(b, off, len);
				writeBuffer();
			} else
				buf.put(b, off, len);
		}

		/**
		 * write a raw buffer
		 * 
		 * @param b buffer to write
		 * @throws IOException in case of ioexception
		 */
		public synchronized void write(ByteBuffer b) throws IOException {
			if (h == null)
				throw new IOException("already closed");
			writeBuffer();
			h.write(b);
		}

		@Override
		public synchronized void close() throws IOException {
			if (h == null)
				return;
			flush();
			h = null;
		}

		/**
		 * check if this co is closed
		 * 
		 * @return true if the co is closed
		 */
		public synchronized boolean isClosed() {
			return h == null;
		}

		@Override
		public synchronized void flush() throws IOException {
			if (h == null)
				return;
			writeBuffer();
		}

		private void writeBuffer() throws IOException {
			if (h == null || buf.position() == 0)
				return;
			h.write(buf.flip());
			buf = ByteBuffer.allocate(BUF_SIZE);
		}
	}

	private class WriteCheck implements WorkerTask {
		@Override
		public void run(long now) {
			if (hasPendingWrites())
				key.interestOpsOr(SelectionKey.OP_WRITE);
			else
				writeScheduled.set(false);
		}
	}
}
