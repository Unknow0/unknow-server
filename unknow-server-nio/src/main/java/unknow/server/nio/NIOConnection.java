/**
 * 
 */
package unknow.server.nio;

import java.io.IOException;
import java.io.InputStream;
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

//	private final Pool<NIOConnection> pool;

	/** the data waiting to be wrote */
	public final Buffers pendingWrite = new Buffers();
	/** the data waiting to be handled */
	public final Buffers pendingRead = new Buffers();

	/** Stream of pending data */
	private final InputStream in = new BuffersInputStream(pendingRead);

	/** Output stream */
	private Out out;

	/** selection key */
	private SelectionKey key;

	private long lastRead;
	private long lastWrite;

//	protected NIOConnection(Pool<NIOConnection> pool) {
//		this.pool = pool;
//	}

	final void init(SelectionKey key) {
		this.key = key;
		this.out = new Out(this);
		lastRead = lastWrite = System.currentTimeMillis();
		onInit();
	}

	/**
	 * called after the connection is initialized
	 */
	protected void onInit() { // for override
	}

	/**
	 * called after some data has been read
	 * @throws InterruptedException
	 * @throws IOException
	 */
	protected void onRead() throws InterruptedException, IOException { // for override
	}

	/**
	 * called after data has been written
	 * @throws InterruptedException
	 * @throws IOException
	 */
	protected void onWrite() throws InterruptedException, IOException { // for override
	}

	/**
	 * called when the connection is free
	 * @throws InterruptedException
	 * @throws IOException
	 */
	protected void onFree() throws InterruptedException, IOException { // for override
	}

	/**
	 * read data from the channel and try to handles it
	 * 
	 * @param channel source channel
	 * @param buf     output buffer
	 * @throws IOException          in case of error
	 * @throws InterruptedException
	 */
	protected final void readFrom(SocketChannel channel, ByteBuffer buf) throws IOException, InterruptedException {
		int l;
		while (true) {
			l = channel.read(buf);
			if (l == -1) {
				channel.close();
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
	}

	/**
	 * write pending data to the channel
	 * 
	 * @param channel where to write
	 * @param buf     local cache
	 * @throws IOException          in case of error
	 * @throws InterruptedException
	 */
	protected final void writeInto(SocketChannel channel, ByteBuffer buf) throws IOException, InterruptedException {
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
			if (buf.hasRemaining())
				break;
		}
		if (buf.hasRemaining()) // we didn't write all
			pendingWrite.prepend(buf);
		toggleKeyOps();
		onWrite();
	}

	private void toggleKeyOps() {
		key.interestOps(pendingWrite.isEmpty() ? SelectionKey.OP_READ : SelectionKey.OP_READ | SelectionKey.OP_WRITE);
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

	/**
	 * @return the current inputStream
	 */
	public final InputStream getIn() {
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
	@SuppressWarnings("resource")
	public final InetSocketAddress getRemote() {
		try {
			return (InetSocketAddress) ((SocketChannel) key.channel()).getRemoteAddress();
		} catch (@SuppressWarnings("unused") IOException e) {
			return null;
		}
	}

	/**
	 * the local address of the channel this handler has
	 * 
	 * @return the local address
	 */
	@SuppressWarnings("resource")
	public final InetSocketAddress getLocal() {
		try {
			return (InetSocketAddress) ((SocketChannel) key.channel()).getLocalAddress();
		} catch (@SuppressWarnings("unused") IOException e) {
			return null;
		}
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
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public final void free() throws InterruptedException, IOException {
		out.close();
		pendingWrite.clear();
		pendingRead.clear();
		onFree();
//		pool.free(this);
	}

	@SuppressWarnings("resource")
	@Override
	public String toString() {
		return key.channel().toString() + " closed: " + isClosed() + " pending: " + pendingWrite.length();
	}

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

		/**
		 * write data
		 * 
		 * @param b data to write
		 * @throws IOException
		 */
		@Override
		public final void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		/**
		 * write some data
		 * 
		 * @param buf data
		 * @param off offset
		 * @param len number of byte to write
		 * @throws IOException
		 */
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

		/**
		 * request to close the handler
		 * 
		 */
		@Override
		public synchronized void close() {
			flush();
			h = null;
		}

		public synchronized boolean isClosed() {
			return h == null;
		}

		/**
		 * flush pending data
		 * 
		 */
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
