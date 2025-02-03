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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersInputStream;

/**
 * used to handle raw data
 * 
 * @author unknow
 */
public abstract class NIOConnectionAbstract {
	private static AtomicInteger COUNTER = new AtomicInteger(0);
	private static final IntUnaryOperator UPDATE = value -> (value + 1) % 238328;

	private static final InetSocketAddress DISCONECTED = InetSocketAddress.createUnresolved("", 0);

	/** the data waiting to be wrote */
	protected final Buffers pendingWrite = new Buffers();
	/** the data waiting to be handled */
	protected final Buffers pendingRead = new Buffers();

	/** Stream of pending data */
	protected final BuffersInputStream in = new BuffersInputStream(pendingRead);

	/** Output stream */
	protected final Out out;

	protected final NIOWorker worker;
	/** selection key */
	protected final SelectionKey key;
	protected final SocketChannel channel;

	protected final NIOConnectionHandler handler;

	protected final InetSocketAddress local;
	protected final InetSocketAddress remote;

	protected final String id;

	protected long lastAction;

	/**
	 * create new connection
	 * @param key the selectionKey
	 * @param handler the handler
	 */
	protected NIOConnectionAbstract(NIOWorker worker, SelectionKey key, NIOConnectionHandler handler) {
		this.worker = worker;
		this.key = key;
		this.channel = (SocketChannel) key.channel();
		this.handler = handler;
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

		StringBuilder sb = new StringBuilder();
		encode(sb, System.currentTimeMillis());
		encode(sb, local.getPort());
		encode(sb, local.getAddress().getAddress());
		encode(sb, COUNTER.getAndUpdate(UPDATE));
		id = sb.toString();
	}

	/** 
	 * get the connection unique id
	 * 
	 * @return the co id
	 */
	public final String getId() {
		return id;
	}

	/**
	 * initialize the connection
	 * @throws InterruptedException
	 * @throws IOException
	 */
	protected abstract void onInit() throws InterruptedException, IOException;

	/**
	 * read data from the channel and try to handles it
	 * 
	 * @param buf output buffer
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	protected abstract void readFrom(ByteBuffer buf) throws InterruptedException, IOException;

	/**
	 * write pending data to the channel
	 * 
	 * @param buf local cache
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	protected abstract void writeInto(ByteBuffer buf) throws InterruptedException, IOException;

	public void toggleKeyOps() {
		if (pendingWrite.isEmpty())
			key.interestOpsAnd(~SelectionKey.OP_WRITE);
		else
			key.interestOpsOr(SelectionKey.OP_WRITE);
	}

	@SuppressWarnings("resource")
	public void flush() {
		if (pendingWrite.isEmpty() || !key.isValid())
			return;
		toggleKeyOps();
		key.selector().wakeup();
	}

	public final void close() {
		worker.close(this);
	}

	public Buffers pendingRead() {
		return pendingRead;
	}

	public Buffers pendingWrite() {
		return pendingWrite;
	}

	/**
	 * @return the current inputStream
	 */
	public final BuffersInputStream getIn() {
		return in;
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
	 * the timestamp of the last read/write
	 * @return the timestamp
	 */
	public final long lastAction() {
		return lastAction;
	}

	/**
	 * check if this connection is closed
	 * 
	 * @return true if we should close this handler
	 */
	public boolean isClosed() {
		return out.isClosed() && pendingWrite.isEmpty();
	}

	/**
	 * check if the connection is closed and should be stoped
	 * 
	 * @param now System.currentMillis()
	 * @param stop if true the server is in stop phase
	 * @return true is the collection is closed
	 */
	public boolean closed(long now, boolean stop) {
		return handler.closed(now, stop);
	}

	/**
	 * free the handler
	 * 
	 * @throws IOException on io error
	 */
	public final void free() throws IOException {
		pendingWrite.clear();
		pendingRead.clear();
		handler.onFree();
	}

	@Override
	public final String toString() {
		boolean closed = false;
		try {
			closed = closed(System.currentTimeMillis(), false);
		} catch (@SuppressWarnings("unused") Exception e) { // ok
		}
		boolean valid = key == null ? false : key.isValid();
		return getClass().getSimpleName() + "[id=" + id + " local=" + local + " remote=" + remote + " valid=" + valid + " closed=" + closed + "]";
	}

	private static final char[] CHAR = { //
			'N', '1', 'l', 'A', 'x', 'b', 'n', 'q', '3', 'B', //
			'j', 't', '6', 'O', 'e', 'R', 'J', '9', '2', 'm', //
			'W', 'z', 'h', 'D', 'i', '0', '4', 'o', '5', 'T', //
			'r', 'H', 'k', 'G', 'I', 'd', 'Z', 'U', 'C', 'Y', //
			'S', 'p', 'L', 'K', 's', 'y', 'c', 'a', 'Q', 'F', //
			'u', 'X', 'M', 'v', 'g', 'E', '7', 'w', 'f', '8', //
			'P', 'V', };

	private static void encode(StringBuilder sb, long value) {
		do {
			sb.append(CHAR[(int) (value % 62)]);
			value /= 62;
		} while (value > 0);
	}

	private static void encode(StringBuilder sb, byte[] value) {
		long l = 0;
		int b = 0;
		for (int i = 0; i < value.length; i++) {
			l += (value[i] & 0xFF) << b;
			if ((b += 8) == 64) {
				encode(sb, l);
				l = 0;
				b = 0;
			}
		}
		if (b > 0)
			encode(sb, l);
	}

	/** output stream for this connection */
	public static final class Out extends OutputStream {
		private NIOConnectionAbstract h;

		private Out(NIOConnectionAbstract h) {
			this.h = h;
		}

		@Override
		public final synchronized void write(int b) throws IOException {
			if (h == null)
				throw new IOException("already closed");
			try {
				h.pendingWrite.write(b);
				h.toggleKeyOps();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			}
		}

		@Override
		public final void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public final synchronized void write(byte[] buf, int off, int len) throws IOException {
			if (len == 0)
				return;
			if ((off | len | (off + len) | (buf.length - (off + len))) < 0)
				throw new IndexOutOfBoundsException();
			if (h == null)
				throw new IOException("already closed");
			try {
				h.pendingWrite.write(buf, off, len);
				h.toggleKeyOps();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			}
		}

		@Override
		public synchronized void close() {
			flush();
			if (h != null)
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
		public synchronized void flush() {
			if (h != null)
				h.flush();
		}
	}
}
