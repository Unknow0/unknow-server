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

import unknow.server.util.io.Buffers;
import unknow.server.util.io.BuffersInputStream;

/**
 * used to handle raw data
 * 
 * @author unknow
 */
public abstract class NIOConnectionAbstract {

	private static final InetSocketAddress DISCONECTED = InetSocketAddress.createUnresolved("", 0);

	/** the data waiting to be wrote */
	protected final Buffers pendingWrite = new Buffers();
	/** the data waiting to be handled */
	protected final Buffers pendingRead = new Buffers();

	/** Stream of pending data */
	protected final BuffersInputStream in = new BuffersInputStream(pendingRead);

	/** Output stream */
	protected final Out out;

	/** selection key */
	protected final SelectionKey key;
	protected final SocketChannel channel;

	protected final NIOConnectionHandler handler;

	protected final InetSocketAddress local;
	protected final InetSocketAddress remote;

	protected long lastRead;
	protected long lastWrite;

	long lastCheck;
	NIOConnectionAbstract next;
	NIOConnectionAbstract prev;

	/**
	 * create new connection
	 * 
	 * @param key the selectionKey
	 * @param handler the handler
	 */
	protected NIOConnectionAbstract(SelectionKey key, long now, NIOConnectionHandler handler) {
		this.key = key;
		this.channel = (SocketChannel) key.channel();
		this.handler = handler;
		this.out = new Out(this);
		lastRead = lastWrite = now;
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

	protected abstract void onInit() throws InterruptedException;

	/**
	 * read data from the channel and try to handles it
	 * 
	 * @param buf output buffer
	 * @param now currentTimeMillis
	 * @return  true if something is read
	 * 
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	protected abstract boolean readFrom(ByteBuffer buf, long now) throws InterruptedException, IOException;

	/**
	 * write pending data to the channel
	 * 
	 * @param buf local cache
	 * @param now currentTimeMillis
	 * @throws InterruptedException on interrupt
	 * @throws IOException on io exception
	 */
	protected abstract void writeInto(ByteBuffer buf, long now) throws InterruptedException, IOException;

	@SuppressWarnings("resource")
	public final void toggleKeyOps() {
		int desired = pendingWrite.isEmpty() ? SelectionKey.OP_READ : SelectionKey.OP_READ | SelectionKey.OP_WRITE;
		if (desired != key.interestOps()) {
			key.interestOps(desired);
			key.selector().wakeup();
		}
	}

	public final void flush() {
		if (pendingWrite.isEmpty())
			return;
		toggleKeyOps();
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
	 * @param now currentTimeMillis
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
		key.cancel();
		try {
			key.channel().close();
		} catch (@SuppressWarnings("unused") IOException e) { // ignore
		}
		out.close();
		pendingWrite.clear();
		pendingRead.clear();
		handler.onFree();
	}

	@Override
	public String toString() {
		return getClass() + "[local=" + local + " remote=" + remote + "]";
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
