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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import unknow.server.nio.NIOWorker.WorkerTask;

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

	protected final InetSocketAddress local;
	protected final InetSocketAddress remote;

	final BlockingQueue<ByteBuffer> pending;
	final ByteBuffer[] writes;
	int writesLength = 0;

	long lastCheck;
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
		this.out = new Out(this);
		InetSocketAddress a;
		try {
			a = (InetSocketAddress) channel.getLocalAddress();
		} catch (@SuppressWarnings("unused") Exception e) {
			a = DISCONECTED;
		}
		local = a;
		try {
			a = (InetSocketAddress) channel.getRemoteAddress();
		} catch (@SuppressWarnings("unused") Exception e) {
			a = DISCONECTED;
		}
		remote = a;

		this.pending = new ArrayBlockingQueue<>(16);
		this.writes = new ByteBuffer[16];
	}

	public final <T> Future<T> submit(Runnable r) {
		return worker.submit(r);
	}

	public final void execute(WorkerTask task) {
		worker.execute(task);
	}

	public boolean hasPendingWrites() {
		return writesLength > 0 || !pending.isEmpty();
	}

	/**
	 * add a buffers to the writing queue
	 * @param buf buffer to be written
	 * @throws InterruptedException  in case of interruption
	 */
	public final void write(ByteBuffer buf) throws InterruptedException {
		if (pending.size() > 10)
			flush();
		pending.put(buf);
		toggleKeyOps();

	}

	public final void toggleKeyOps() {
		if (!key.isValid())
			return;
		if (hasPendingWrites())
			key.interestOpsOr(SelectionKey.OP_WRITE);
		else
			key.interestOpsAnd(~SelectionKey.OP_WRITE);
	}

	@SuppressWarnings("resource")
	public final void flush() {
		if (!hasPendingWrites())
			return;
		toggleKeyOps();
		key.selector().wakeup();
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
		return remote;
	}

	/**
	 * the local address of the channel this handler has
	 * 
	 * @return the local address
	 */
	public final InetSocketAddress getLocal() {
		return local;
	}

	/**
	 * check if this connection is closed
	 * 
	 * @return true if we should close this handler
	 */
	public boolean isClosed() {
		return out.isClosed() && writesLength == 0;
	}

	protected final void beforeWrite() throws IOException {
		while (writesLength < writes.length && !pending.isEmpty()) {
			ByteBuffer b = handler.beforeWrite(pending.poll());
			if (b != null && b.hasRemaining())
				writes[writesLength++] = b;
		}
	}

	@Override
	public final void onWrite(long now) throws IOException {
		int o = 0;
		while (o < writesLength && !writes[o].hasRemaining())
			o++;
		if (o == 0)
			return;

		writesLength -= o;
		if (writesLength > 0)
			System.arraycopy(writes, o, writes, 0, writesLength);
		for (int i = writesLength; i < o; i++)
			writes[i] = null;
		toggleKeyOps();
		handler.onWrite(now);
	}

	@Override
	public void startClose() {
		handler.startClose();
	}

	/**
	 * free the handler
	 */
	@Override
	public final void doneClosing() {
		key.cancel();
		try {
			key.channel().close();
		} catch (@SuppressWarnings("unused") IOException e) { // ignore
		}
		try {
			out.close();
		} catch (@SuppressWarnings("unused") IOException e) { // ignore
		}
		handler.doneClosing();
	}

	@Override
	public String toString() {
		return getClass() + "[local=" + local + " remote=" + remote + "]";
	}

	/** output stream for this connection */
	public static final class Out extends OutputStream {
		private NIOConnection h;

		private ByteBuffer buf;

		private Out(NIOConnection h) {
			this.h = h;
			this.buf = ByteBuffer.allocate(4096);
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
				if (len < 4096)
					buf.put(b, off, len);
				else
					try {
						h.write(ByteBuffer.wrap(Arrays.copyOfRange(b, off, off + len)));
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new IOException(e);
					}
			} else if (len == r) {
				buf.put(b, off, len);
				writeBuffer();
			} else
				buf.put(b, off, len);
		}

		/**
		 * write a raw buffer
		 * @param b buffer to write
		 * @throws IOException in case of ioexception
		 */
		public synchronized void write(ByteBuffer b) throws IOException {
			if (h == null)
				throw new IOException("already closed");
			writeBuffer();
			try {
				h.write(b);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			}
		}

		@Override
		public synchronized void close() throws IOException {
			if (h == null)
				return;
			flush();
			h.onOutputClosed();
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
			writeBuffer();
			if (h != null)
				h.flush();
		}

		private void writeBuffer() throws IOException {
			if (h == null || buf.position() == 0)
				return;
			try {
				h.write(buf.flip());
				buf = ByteBuffer.allocate(4096);
				return;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			}
		}
	}
}
