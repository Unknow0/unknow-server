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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersInputStream;

/**
 * used to handle raw data
 * 
 * @author unknow
 */
public class NIOConnection {
	private static final Logger logger = LoggerFactory.getLogger(NIOConnection.class);

	private static final InetSocketAddress DISCONECTED = InetSocketAddress.createUnresolved("", 0);

	/** the data waiting to be wrote */
	private final Buffers pendingWrite = new Buffers();
	/** the data waiting to be handled */
	private final Buffers pendingRead = new Buffers();

	/** Stream of pending data */
	protected final BuffersInputStream in = new BuffersInputStream(pendingRead);

	/** Output stream */
	protected final Out out;

	/** selection key */
	protected final SelectionKey key;
	protected final SocketChannel channel;

	protected final InetSocketAddress local;
	protected final InetSocketAddress remote;

	protected long lastRead;
	protected long lastWrite;

	/**
	 * create new connection
	 * 
	 * @param key the selectionKey
	 */
	public NIOConnection(SelectionKey key) {
		this.key = key;
		this.channel = (SocketChannel) key.channel();
		this.out = new Out(this);
		lastRead = lastWrite = System.currentTimeMillis();
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
	}

	/**
	 * called after the connection is initialized
	 * 
	 * @throws InterruptedException on interrupt
	 */
	protected void onInit() throws InterruptedException { // for override
	}

	/**
	 * called after some data has been read
	 * 
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	protected void onRead() throws InterruptedException, IOException { // for override
	}

	/**
	 * called after data has been written
	 * 
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	protected void onWrite() throws InterruptedException, IOException { // for override
	}

	/**
	 * called when the connection is free
	 * 
	 * @throws IOException on io exception
	 */
	protected void onFree() throws IOException { // for override
	}

	/**
	 * read data from the channel and try to handles it
	 * 
	 * @param buf output buffer
	 * 
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	protected void readFrom(ByteBuffer buf) throws InterruptedException, IOException {
		int l;
		lastRead = System.currentTimeMillis();
		l = channel.read(buf);
		if (l == -1) {
			in.close();
			return;
		}
		if (l == 0)
			return;
		buf.flip();

		if (logger.isTraceEnabled()) {
			buf.mark();
			byte[] bytes = new byte[buf.remaining()];
			buf.get(bytes);
			logger.trace("read {}", new String(bytes));
			buf.reset();
		}
		pendingRead.write(buf);
		onRead();
	}

	/**
	 * write pending data to the channel
	 * 
	 * @param buf local cache
	 * 
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	protected void writeInto(ByteBuffer buf) throws InterruptedException, IOException {
		lastWrite = System.currentTimeMillis();
		while (pendingWrite.read(buf, false)) {
			buf.flip();

			if (logger.isTraceEnabled()) {
				buf.mark();
				byte[] bytes = new byte[buf.remaining()];
				buf.get(bytes);
				logger.trace("writ {}", new String(bytes));
				buf.reset();
			}

			channel.write(buf);
			if (buf.hasRemaining()) {
				pendingWrite.prepend(buf);
				break;
			}
		}
		toggleKeyOps();
		onWrite();
	}

	public void toggleKeyOps() {
		key.interestOps(pendingWrite.isEmpty() ? SelectionKey.OP_READ : SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}

	@SuppressWarnings("resource")
	public void flush() {
		if (pendingWrite.isEmpty())
			return;
		toggleKeyOps();
		key.selector().wakeup();
	}

	/**
	 * timestamp of the last read
	 * 
	 * @return timestamp in ms
	 */
	public final long lastRead() {
		return lastRead;
	}

	/**
	 * timestamp of the last write
	 * 
	 * @return timestamp in ms
	 */
	public final long lastWrite() {
		return lastWrite;
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
	@SuppressWarnings("unused")
	public boolean closed(long now, boolean stop) {
		return isClosed();
	}

	/**
	 * free the handler
	 * 
	 * @throws IOException on io error
	 */
	public final void free() throws IOException {
		out.close();
		pendingWrite.clear();
		pendingRead.clear();
		onFree();
	}

	@Override
	public String toString() {
		return getClass() + "[local=" + local + " remote=" + remote + "]";
	}

	/** output stream for this connection */
	public static final class Out extends OutputStream {
		private NIOConnection h;

		private Out(NIOConnection h) {
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

		@SuppressWarnings("resource")
		@Override
		public synchronized void flush() {
			if (h == null)
				return;
			if (h.key.isValid())
				h.toggleKeyOps();
			if (!h.pendingWrite.isEmpty())
				h.key.selector().wakeup();
		}
	}
}
