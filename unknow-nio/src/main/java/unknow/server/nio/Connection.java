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

import unknow.server.nio.util.Buffers;
import unknow.server.nio.util.BuffersInputStream;

/**
 * used to handle raw data
 * 
 * @author unknow
 */
public final class Connection {
	/** factory that created the handler */
	private final Handler handler;

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

	protected Connection(HandlerFactory factory) {
		this.handler = factory.create(this);
	}

	void attach(SelectionKey key) {
		key.attach(this);
		this.key = key;
		this.out = new Out(this);
		lastRead = lastWrite = System.currentTimeMillis();
	}

	/**
	 * read data from the channel and try to handles it
	 * 
	 * @param channel source channel
	 * @param buf     output buffer
	 * @throws IOException in case of error
	 */
	protected final void readFrom(SocketChannel channel, ByteBuffer buf) throws IOException {
		lastRead = System.currentTimeMillis();
		buf.clear();
		if (channel.read(buf) == -1) {
			channel.close();
			return;
		}
		buf.flip();
		pendingRead.write(buf);
		handler.onRead();
	}

	/**
	 * write pending data to the channel
	 * 
	 * @param channel where to write
	 * @param buf     local cache
	 * @throws IOException in case of error
	 */
	protected final void writeInto(SocketChannel channel, ByteBuffer buf) throws IOException {
		lastWrite = System.currentTimeMillis();
		buf.clear();
		while (pendingWrite.read(buf)) {
			buf.flip();
			channel.write(buf);
			if (buf.hasRemaining())
				break;
		}
		if (buf.hasRemaining()) // we didn't write all
			pendingWrite.prepend(buf);
		if (pendingWrite.isEmpty())
			key.interestOps(SelectionKey.OP_READ);
		handler.onWrite();
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
	public final OutputStream getOut() {
		return out;
	}

	/**
	 * the remote address of the channel is connected to
	 * 
	 * @return the remote address
	 */
	public final InetSocketAddress getRemote() {
		try {
			return (InetSocketAddress) ((SocketChannel) key.channel()).getRemoteAddress();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * the local address of the channel this handler has
	 * 
	 * @return the local address
	 */
	public final InetSocketAddress getLocal() {
		try {
			return (InetSocketAddress) ((SocketChannel) key.channel()).getLocalAddress();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * check if this connection is closed
	 * 
	 * @return true if we should close this handler
	 */
	public boolean isClosed() {
		return out.h == null && pendingWrite.isEmpty();
	}

	public boolean closed(long now, boolean stop) {
		return handler.closed(now, stop);
	}

	/**
	 * free the handler
	 */
	public final void free() {
		reset();
		handler.free();
	}

	/**
	 * reset this handler
	 */
	public void reset() {
		try {
			out.close();
		} catch (IOException e) { // OK
		}
		pendingWrite.clear();
		pendingRead.clear();
		handler.reset();
	}

	@Override
	public String toString() {
		return key.channel().toString() + " closed: " + isClosed() + " pending: " + pendingWrite.length();
	}

	private static final class Out extends OutputStream {
		private volatile Connection h;

		Out(Connection h) {
			this.h = h;
		}

		@Override
		public synchronized final void write(int b) throws IOException {
			if (h == null)
				throw new IOException("already closed");
			h.pendingWrite.write(b);
			h.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
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
		public synchronized final void write(byte[] buf, int off, int len) throws IOException {
			if (len == 0)
				return;
			if ((off | len | (off + len) | (buf.length - (off + len))) < 0)
				throw new IndexOutOfBoundsException();
			if (h == null)
				throw new IOException("already closed");
			h.pendingWrite.write(buf, off, len);
			h.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}

		/**
		 * request to close the handler
		 * 
		 * @throws IOException
		 */
		@Override
		public synchronized void close() throws IOException {
			flush();
			h = null;
		}

		/**
		 * flush pending data
		 * 
		 * @throws IOException
		 */
		@Override
		public synchronized void flush() throws IOException {
			if (h == null)
				return;
			if (!h.pendingWrite.isEmpty())
				h.key.selector().wakeup();
		}
	}
}
